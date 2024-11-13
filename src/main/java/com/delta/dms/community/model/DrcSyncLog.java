package com.delta.dms.community.model;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class DrcSyncLog {
    private int id;
    private String action;
    private int topicId;
    private int communityId;
    private int forumId;
    private Timestamp syncTime;
    private int responseCode;
    private String responseBody;
}
