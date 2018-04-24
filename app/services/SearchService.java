package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import controllers.Search;
import domains.Sku;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import play.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * @author bowen
 */
public class SearchService {

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

        List<Map<Object, String>> documents = Lists.newArrayList();

        for (Map<Object, String> document : documents) {
            IndexRequest indexRequest = new IndexRequest(INDEX, TYPE, document.get("id"));
            String jsonString = MAPPER.writeValueAsString(document);
            indexRequest.source(jsonString, XContentType.JSON);
            bulkRequestBuilder.add(indexRequest);
        }
        BulkResponse br = bulkRequestBuilder.execute().actionGet();
        if (!br.hasFailures()) {
            client.admin().indices().prepareRefresh().execute().actionGet();
        }
    }

    //protected void putExtraFields(Map<String, Object> map, House entity) {
    //    map.put("keywords", String.format("%s,%s,%s,%s", entity.communityName, entity.areaName, entity.businessAreaName, entity.description));
    //}

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

    public static void main(String[] args) throws Exception {
        SearchService search = new SearchService();
        search.setup();
        search.deleteIndex();
        search.rebuildIndex();
        search.release();
        System.out.println(System.currentTimeMillis());
    }

}
