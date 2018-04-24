package controllers;

import com.google.common.collect.Sets;
import org.elasticsearch.search.sort.SortOrder;

import java.util.Set;

/**
 * @author bowen
 */
public class Query {

    public String keywords;

    public Integer categoryId;

    public Set<Integer> brandIds = Sets.newHashSet();

    public SortOrder sortOrder;

    public int start;

    public int limit;

}
