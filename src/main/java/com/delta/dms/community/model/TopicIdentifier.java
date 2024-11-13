package com.delta.dms.community.model;

public class TopicIdentifier {
    private Integer communityId;
    private Integer topicId;

    public TopicIdentifier(Integer communityId, Integer topicId) {
        this.communityId = communityId;
        this.topicId = topicId;
    }

    // Getters and setters
    public Integer getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Integer communityId) {
        this.communityId = communityId;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
    }
}

