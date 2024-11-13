/***********************************************************
 *  Created: 2024/03
 *  Author: MARK.TSAO
 *  Goal: 透過此Listener處理 DrcSyncEvent
 */
package com.delta.dms.community.listener;

import com.delta.dms.community.event.DrcSyncEvent;
import com.delta.dms.community.adapter.DrcAdapter;
import com.delta.dms.community.model.DrcSyncSignIn;
import com.delta.set.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DrcSyncEventListener {
    private final DrcAdapter drcAdapter;
    private static final LogUtil log = LogUtil.getInstance();

    @Autowired
    public DrcSyncEventListener(DrcAdapter drcAdapter) {
        this.drcAdapter = drcAdapter;
    }

    /**
     * 依照 Action 打對應的 drc Sync Api
     *
     * @return      API Response status code, and body
     */
    @Async
    @TransactionalEventListener
    public void handleDrcSyncEvent(DrcSyncEvent event) {
        switch (event.getAction()) {
            case CREATE:
                processCreateAction(event);
                break;
            case UPDATE:
                processUpdateAction(event);
                break;
            case DELETE:
                processDeleteAction(event);
                break;
        }
    }

    private void processCreateAction(DrcSyncEvent event) {
        DrcSyncSignIn signIn = drcAdapter.signIn();
        drcAdapter.upsertTopic(event.getCommunityId(), event.getForumId(), event.getTopicId(), event.getTopicTitle(), event.getTopicText(), event.getAction().toString(), signIn.getTokens().getAccessToken());
    }

    private void processUpdateAction(DrcSyncEvent event) {
        DrcSyncSignIn signIn = drcAdapter.signIn();
        drcAdapter.upsertTopic(event.getCommunityId(), event.getForumId(), event.getTopicId(), event.getTopicTitle(), event.getTopicText(), event.getAction().toString(), signIn.getTokens().getAccessToken());
    }

    private void processDeleteAction(DrcSyncEvent event) {
        DrcSyncSignIn signIn = drcAdapter.signIn();
        drcAdapter.deleteTopic(event.getCommunityId(), event.getForumId(), event.getTopicId(), event.getAction().toString(), signIn.getTokens().getAccessToken());
    }
}
