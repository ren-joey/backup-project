package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;

public class DDFDeleteQueueTriggerDeletingEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;
    private int associatedId;

    public DDFDeleteQueueTriggerDeletingEvent(Object source, int associatedId) {
        super(source);
        this.associatedId = associatedId;
    }

    public int getAssociatedId() {
        return associatedId;
    }
}

