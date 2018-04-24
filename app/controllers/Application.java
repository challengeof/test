package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import domains.Category;
import domains.Manager;
import domains.Sku;
import enums.ErrorCode;
import lib.BizException;
import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import play.Logger;
import play.db.jpa.JPA;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class Application extends BasicController {

    private Client client;

    private static ObjectMapper MAPPER = new ObjectMapper();

    private static String INDEX = "product-index";

    private static String TYPE = "index";

    @PostConstruct
    public void setup() throws UnknownHostException {

        Settings settings = Settings.builder()
                .put("cluster.name", "my-application").build();

        client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new InetSocketTransportAddress(
                        InetAddress.getByName("127.0.0.1"), 9300));
    }

    @PreDestroy
    public void release() {
        client.close();
    }

    private void deleteIndex() {
        ClusterStateResponse response = client.admin().cluster()
                .prepareState()
                .execute().actionGet();
        String[] indices = response.getState().getMetaData().getConcreteAllIndices();
        for (String index : indices) {
            System.out.println(index + " delete");
            client.admin().indices()
                    .prepareDelete(index)
                    .execute().actionGet();

        }

    }

    public void rebuildIndex() throws Exception {
        client.admin().indices().prepareCreate(INDEX).execute().actionGet();
        XContentBuilder contentBuilder = buildMapping();
        Logger.info("Mapping is : {}", contentBuilder.string());
        PutMappingResponse response = client
                .admin()
                .indices()
                .preparePutMapping(INDEX)
                .setType(TYPE)
                .setSource(contentBuilder)
                .execute()
                .actionGet();
        client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
        BulkRequestBuilder bulkRequestBuilder = client.prepareBulk();

        List<Sku> skuList = Sku.findAll();
        for (Sku sku : skuList) {
            String categorySql = String.format("select c.ST_NAME, c.ST_ID, c.PARENT_ID from GAM_SHOWTYPE_SPU ck left join GAM_SHOWTYPE c on ck.ST_ID = c.ST_ID where ck.SKUID = %s", sku.skuId);
            List<Object[]> categories = JPA.em().createNativeQuery(categorySql).getResultList();
            Map<String, Object> document = Maps.newHashMap();
            for (Object[] category : categories) {

                IndexRequest indexRequest = new IndexRequest(INDEX, TYPE);
                document = sku.toMap();
                Category parentCategory = Category.findById(category[2]);
                document.put("keywords", String.format("%s,%s,%s", sku.name, category[0], parentCategory.name));
                document.put("categoryName", category[0]);
                document.put("categoryId", category[1]);

                String brandSql = String.format("select b.BR_NAME, b.ORDER_VAL from GAM_GDBRAND b left join GAM_SKU s on b.BR_ID = s.BR_ID where s.SKUID = %s", sku.skuId);
                List<Object[]> brands = JPA.em().createNativeQuery(brandSql).getResultList();

                for (Object[] brand : brands) {
                    document.put("keywords", String.format("%s,%s", document.get("keywords"), brand[0]));
                    document.put("brandName", brand[0]);
                    document.put("orderQ", brand[1]);
                }

                String jsonString = MAPPER.writeValueAsString(document);
                indexRequest.source(jsonString, XContentType.JSON);
                bulkRequestBuilder.add(indexRequest);
            }


        }
        BulkResponse br = bulkRequestBuilder.execute().actionGet();
        if (!br.hasFailures()) {
            client.admin().indices().prepareRefresh().execute().actionGet();
        }
    }

    private static XContentBuilder buildMapping() throws Exception {
        return jsonBuilder().prettyPrint()
                .startObject()
                .startObject(TYPE)
                .startObject("properties")
                .startObject("keywords")
                .field("type", "text")
                .field("analyzer", "ik_max_word")
                .endObject()
                .endObject()
                .endObject()
                .endObject();
    }

    public List<Map<String, Object>> search(Query query) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        if (query.start < 0) {
            query.start = 0;
        }

        if (query.limit < 0 || query.limit > 1000) {
            query.limit = 1000;
        }

        if (!Strings.isNullOrEmpty(query.keywords)) {
            MatchPhraseQueryBuilder queryBuilder = QueryBuilders.matchPhraseQuery("keywords", query.keywords);
            boolQueryBuilder.must(queryBuilder);
        }

        if (CollectionUtils.isNotEmpty(query.brandIds) && query.brandIds.size() > 0) {
            BoolQueryBuilder statusQueryBuilder = new BoolQueryBuilder();
            for (Integer brandId : query.brandIds) {
                statusQueryBuilder.should(QueryBuilders.termQuery("brandId", brandId));
            }
            boolQueryBuilder.must(statusQueryBuilder);
        }

        if (query.categoryId != null && query.categoryId != 0) {
            boolQueryBuilder.must(QueryBuilders.termQuery("categoryId", query.categoryId));
        }

        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(INDEX).setTypes(TYPE)
                .setQuery(boolQueryBuilder)
                .setFrom(query.start)
                .setSize(query.limit)
                .addSort(SortBuilders.fieldSort("salePrice").order(query.sortOrder));

        SearchResponse resp = searchRequestBuilder.execute().actionGet();
        List<Map<String, Object>> res = Lists.newArrayList();
        for (SearchHit searchHitFields : resp.getHits().getHits()) {
            res.add(searchHitFields.getSource());
        }
        return res;


    }

    public static void search(String keywords, Set<Integer> brandIds, Integer categoryId) throws Exception {
        Application search = new Application();
        search.setup();
        Query query = new Query();
        query.keywords = keywords;
        query.brandIds = brandIds;
        query.categoryId = categoryId;
        query.sortOrder = SortOrder.ASC;
        query.start = 0;
        query.limit = 100;
        List<Map<String, Object>> result = search.search(query);
        Set<Map<String, Object>> brands = Sets.newHashSet();
        Set<Map<String, Object>> categories = Sets.newHashSet();
        Set<Map<String, Object>> products = Sets.newLinkedHashSet();
        for (Map<String, Object> map : result) {
            brands.add(ImmutableMap.of("brandId", map.get("brandId"), "brandName", map.get("brandName")));
            categories.add(ImmutableMap.of("categoryId", map.get("categoryId"), "categoryName", map.get("categoryName")));
            products.add(ImmutableMap.of("skuId", map.get("skuId"), "gdName", map.get("name"), "salePrice", map.get("salePrice"), "saleNum", map.get("saleNum")));
        }

        Map<String, Object> res = ImmutableMap.of("brands", brands, "categories", categories, "products", products);
        ok(res);
    }

    public static void index() {
        renderTemplate();
    }

    public static void login(String username, String password) {
        // TODO: 2018/4/18 理论上密码要加密 
        Manager manager = Manager.find("name = ? and password = ?", username, password).first();
        if (manager == null) {
            err(new BizException(ErrorCode._10001));
        }
        String sql = String.format("SELECT usr_name, prc_name, MAX(sign_time),CASE WHEN TIMEDIFF(TIME(MAX(sign_time)), '08:30:00') > 0 THEN '是' ELSE '否' END AS '是否迟到', COUNT(sign_id) AS times FROM (\n" +
                "SELECT u.usr_name, pro.prc_name, s.sign_time, s.id AS sign_id, u.usr_id, s.prc_id FROM sign s LEFT JOIN usr u ON s.usr_id = u.usr_id LEFT JOIN project pro ON s.prc_id = pro.prc_id " +
                "LEFT JOIN privilege p ON p.prc_id = pro.prc_id LEFT JOIN manager m ON m.mng_id = p.mng_id WHERE m.mng_id = %s ORDER BY s.sign_time DESC\n" +
                ") a\n" +
                "GROUP BY usr_name, prc_name", manager.id);

        List<Object[]> list = JPA.em().createNativeQuery(sql).getResultList();

        renderTemplate(ImmutableMap.of("list", list));
    }

}