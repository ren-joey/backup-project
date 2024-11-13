/***********************************************************
 *  Created: 2024/03
 *  Author: MARK.TSAO
 *  Goal: 透過此service處理Community相關Sync
 */
package com.delta.dms.community.service;

import com.delta.dms.community.adapter.DrcAdapter;
import com.delta.dms.community.config.DrcSyncConfig;
import com.delta.dms.community.dao.DrcSyncDao;
import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.dao.ForumDao;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.enums.DrcSyncType;
import com.delta.dms.community.model.DrcSyncLog;
import com.delta.dms.community.model.DrcSyncSignIn;
import com.delta.dms.community.swagger.model.SyncResult;
import com.delta.dms.community.swagger.model.TopicIdentifier;
import com.delta.dms.community.utils.Constants;
import com.delta.set.utils.LogUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang.StringUtils.EMPTY;

@RequiredArgsConstructor
@Service
@Transactional
public class CommunitySyncService {
    private static final LogUtil log = LogUtil.getInstance();
    private final DrcSyncConfig drcSyncConfig;
    private final DrcAdapter drcAdapter;
    private final TopicDao topicDao;
    private final ForumDao forumDao;
    private final DrcSyncDao drcSyncDao;
    private final int LIMIT = 20;

    /**
     * 同步特定communityId底下的topic給drc
     *
     * @return      成功sync的topic筆數, 失敗的topic筆數跟topic列表
     */
    public SyncResult drcSync() {
        // 0. Sign in to DRC
        DrcSyncSignIn signIn = drcAdapter.signIn();
        if (null == signIn) {
            return new SyncResult()
                    .successCount(0)
                    .failureCount(0)
                    .failures(new ArrayList<>());
        }
        // 1. 從 cloud-config 抓 community id list
        List<Integer> communityIds = drcSyncConfig.getCommunityId();
        int totalSuccessCounter = 0;
        int totalFailCounter = 0;
        List<TopicIdentifier> totalFailList = new ArrayList<>();
        // 2. 遞迴 community id list
        for (Integer communityId : communityIds) {
            int offset = 0;
            List<TopicInfo> topics;
            do {
                // 2.1. 從 db 拿取 此 community Id 底下的 topics
                topics = topicDao.getTopicOfCommunityWithSortAndLimit(
                        communityId,
                        offset,
                        LIMIT,
                        "topic_id",
                        EMPTY,
                        true, null, null, null, false, null
                );
                log.debug("topicDao.getTopicOfCommunityWithSortAndLimit: " + topics);
                // 2.2. 遞迴 topics
                for (TopicInfo topic : topics) {
                    // 2.2.1 Get topic's forum's forum_status by calling ForumDAO by using topic_id
                    ForumInfo forumInfo = forumDao.getForumById(topic.getForumId(), Constants.EN_US);
                    // 2.2.2 if topic's forum+status == public we can give topicInfo to DRC
                    if (null == forumInfo || !forumInfo.isPublicForum()) {
                        continue;
                    }
                    log.debug("topic: " + topic);
                    log.debug("topic.getTopicText: " + topic.getTopicText());
                    // 2.2.3 拿到 topic 文章內容
                    String topicText = topicDao.getTopicInfoById(topic.getTopicId()).getTopicText();
                    // 2.2.4 upsert檔案給 drc
                    AbstractMap.SimpleEntry<Integer, String> uploadResult = drcAdapter.upsertTopic(
                            communityId, topic.getForumId(), topic.getTopicId(), topic.getTopicTitle(),
                            topicText, DrcSyncType.BATCH_UPSERT.toString(), signIn.getTokens().getAccessToken());
                    // 因為檔案內容重複時會回傳409，代表此檔案無更新內容，所以視為成功
                    if (uploadResult.getKey() == HttpStatus.OK.value() || uploadResult.getKey() == HttpStatus.CONFLICT.value()) {
                        totalSuccessCounter++;
                    } else {
                        // 2.2.5 上傳更新皆失敗則將此topicId加入倒totalFailList
                        totalFailList.add(new TopicIdentifier().communityId(communityId).topicId(topic.getTopicId()));
                        totalFailCounter++;
                    }
                }
                offset += LIMIT;
            } while (!topics.isEmpty());
        }

        return new SyncResult()
                .successCount(totalSuccessCounter)
                .failureCount(totalFailCounter)
                .failures(totalFailList);
    }

    public SyncResult deleteSynchronizedCommunityTopics(Date startTime, Date endTime) {
        // 0. Sign in to DRC
        DrcSyncSignIn signIn = drcAdapter.signIn();
        if (null == signIn) {
            return new SyncResult()
                    .successCount(0)
                    .failureCount(0)
                    .failures(new ArrayList<>());
        }
        AtomicInteger totalSuccessCounter = new AtomicInteger();
        List<DrcSyncLog> logs = drcSyncDao.findDrcSyncLogsForSpecificActions(startTime, endTime);
        logs.forEach(drcSyncLog -> {
            AbstractMap.SimpleEntry<Integer, String> deleteResult = drcAdapter.deleteTopic(
                    drcSyncLog.getCommunityId(),
                    drcSyncLog.getForumId(),
                    drcSyncLog.getTopicId(),
                    DrcSyncType.BATCH_DELETE.toString(),
                    signIn.getTokens().getAccessToken()
            );
            if (deleteResult.getKey() == HttpStatus.OK.value() || deleteResult.getKey() == HttpStatus.CONFLICT.value()) {
                totalSuccessCounter.getAndIncrement();
            }
        });
        return new SyncResult()
                .successCount(totalSuccessCounter.get())
                .failureCount(0)
                .failures(new ArrayList<>());
    }
}


