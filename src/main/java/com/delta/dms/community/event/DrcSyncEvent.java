package com.delta.dms.community.event;

import com.delta.dms.community.enums.DrcSyncType;
import com.delta.dms.community.swagger.model.TopicCreationData;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DrcSyncEvent extends ApplicationEvent {
    private final String database;
    private final DrcSyncType action;
    private final int topicId;
    private final String topicTitle;
    private final String topicText;
    private final int communityId;
    private final int forumId;

    public DrcSyncEvent(Object source, String database, DrcSyncType action, int topicId, String topicTitle, String topicText, int communityId, int forumId) {
        super(source);
        this.database = database;
        this.action = action;
        this.topicId = topicId;
        this.topicTitle = topicTitle;
        this.topicText = topicText;
        this.communityId = communityId;
        this.forumId = forumId;
    }
}
