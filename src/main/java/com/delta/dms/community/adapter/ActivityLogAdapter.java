package com.delta.dms.community.adapter;


import com.delta.datahive.searchobj.param.GeneralQuery;
import com.delta.datahive.searchobj.param.Query;
import com.delta.datahive.searchobj.response.SearchResponse;
import com.delta.datahive.searchobj.type.activitylog.ActivityLogResult;
import com.delta.dms.community.config.ServiceConfig;
import com.delta.dms.community.config.YamlConfig;
import com.delta.dms.community.utils.DsmpConstants;
import com.delta.dms.community.utils.TimestampConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ActivityLogAdapter {

    private final ServiceConfig serviceConfig;
    private final YamlConfig yamlConfig;

    public ActivityLogAdapter(ServiceConfig serviceConfig, YamlConfig yamlConfig) {
        this.serviceConfig = serviceConfig;
        this.yamlConfig = yamlConfig;
    }

    public SearchResponse<ActivityLogResult> queryActivityLog(
            String activityType,
            String objectType,
            List<String> objectIds,
            Long startTime,
            Long endTime) {

        List<Query> mandatoryQueries = new ArrayList<>();
        mandatoryQueries.add(new GeneralQuery(DsmpConstants.QUERY_TYPE_ACTIVITY + activityType));
        mandatoryQueries.add(new GeneralQuery(objectType));

        if (!objectIds.isEmpty()) {
            addObjectIdQuery(mandatoryQueries, objectIds);
        }

        if (startTime != null && endTime != null) {
            addTimeRangeQuery(mandatoryQueries, startTime, endTime);
        }

        SearchAdapter searchAdapter = new SearchAdapter(serviceConfig, yamlConfig);
        return searchAdapter.getActivityLog(mandatoryQueries, DsmpConstants.QUERY_LIMIT);
    }

    private void addObjectIdQuery(List<Query> mandatoryQueries, List<String> ids) {
        if (!ids.isEmpty()) {
            String objectIdQueryStr = String.format(
                    DsmpConstants.OBJECT_ID_QUERY_TEMPLATE, String.join(DsmpConstants.OBJECT_ID_DELIMITER, ids));
            GeneralQuery objectIdQuery = new GeneralQuery(objectIdQueryStr);
            mandatoryQueries.add(objectIdQuery);
        }
    }

    private void addTimeRangeQuery(List<Query> mandatoryQueries, Long startTime, Long endTime) {
        if (startTime != null && endTime != null) {
            String startTimeStr = TimestampConverter.convertMillisToISO8601UTC(startTime);
            String endTimeStr = TimestampConverter.convertMillisToISO8601UTC(endTime);
            String timeRangeQueryStr = String.format(DsmpConstants.TIME_RANGE_QUERY_TEMPLATE, startTimeStr, endTimeStr);
            GeneralQuery timeRangeQuery = new GeneralQuery(timeRangeQueryStr);
            mandatoryQueries.add(timeRangeQuery);
        }
    }
}