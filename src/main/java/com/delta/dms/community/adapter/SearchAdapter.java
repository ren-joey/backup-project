package com.delta.dms.community.adapter;


import com.delta.datahive.searchapi.SearchManager;
import com.delta.datahive.searchobj.param.Facet;
import com.delta.datahive.searchobj.param.Query;
import com.delta.datahive.searchobj.param.QuerySettings;
import com.delta.datahive.searchobj.param.SortOrder;
import com.delta.datahive.searchobj.response.SearchResponse;
import com.delta.datahive.searchobj.type.activitylog.ActivityLogResult;
import com.delta.dms.community.config.ServiceConfig;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.model.Jwt;

import java.util.ArrayList;
import java.util.List;


public class SearchAdapter {
    private final SearchManager srchMgr;
    public static final String ACTIVITY_TIME = "activityTime";

    public SearchAdapter(ServiceConfig serviceConfig, YamlConfig yamlConfig) {
        this.srchMgr = new SearchManager(
                yamlConfig.getHost() + serviceConfig.getActivityLog().getSearchservice());
    }

    public SearchResponse<ActivityLogResult> getActivityLog(List<Query> mandatoryQuery, int resultsPerPage) {

        List<ActivityLogResult> allResults = new ArrayList<>();
        int pageNumber = 1;
        SearchResponse<ActivityLogResult> searchResponse = null;

        while(true) {
            QuerySettings qs = getQuerySettings(mandatoryQuery, pageNumber, resultsPerPage);
            searchResponse = srchMgr.activitylogSearch(qs, Jwt.get());
            allResults.addAll(searchResponse.getResults());
            if (searchResponse.getNumOfFound() > allResults.size()) {
                pageNumber++;
            } else {
                break;
            }
        }
        return new SearchResponse<>(
                searchResponse.getNumOfFound(),
                searchResponse.getPageNumber(),
                searchResponse.getOffset(),
                allResults,
                searchResponse.getFacetResults()
        );
    }

    private QuerySettings getQuerySettings(List<Query> mandatoryQuery, int pageNumber, int resultPerPage) {
        QuerySettings qs = new QuerySettings();
        qs.setMandatoryQueries(mandatoryQuery);
        qs.setPageNumber(pageNumber);
        qs.setResultsPerPage(resultPerPage);

        // sort by activityTime desc
        List<SortOrder> sortOrders = new ArrayList<>();
        SortOrder sortOrder = new SortOrder(ACTIVITY_TIME, true);
        sortOrders.add(sortOrder);
        qs.setSortOrders(sortOrders);
        return qs;
    }
}