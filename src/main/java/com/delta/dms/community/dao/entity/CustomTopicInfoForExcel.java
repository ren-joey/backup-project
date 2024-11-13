package com.delta.dms.community.dao.entity;

import lombok.Getter;

@Getter
public class CustomTopicInfoForExcel {
    private final int topicId;
    private final String topicTitle;

    public CustomTopicInfoForExcel(int topicId, String topicTitle) {
        this.topicId = topicId;
        this.topicTitle = topicTitle;
    }
}