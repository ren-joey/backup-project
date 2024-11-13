/***********************************************************
 *  Created: 2024/03
 *  Author: MARK.TSAO
 *  Goal: 透過此controller處理 Community Sync 相關服務
 */
package com.delta.dms.community.controller;

import com.delta.dms.community.service.CommunitySyncService;
import com.delta.dms.community.swagger.controller.CommunitySyncApi;
import com.delta.dms.community.swagger.model.ResponseBean;
import com.delta.dms.community.swagger.model.SyncResult;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.OffsetDateTime;
import java.util.Date;

@Api(
        tags = {
                "CommunitySync",
        })
@RestController
public class CommunitySyncController implements CommunitySyncApi {

    private final CommunitySyncService communitySyncService;

    @Autowired
    public CommunitySyncController(CommunitySyncService communitySyncService) {
        this.communitySyncService = communitySyncService;
    }

    @Override
    public ResponseBean<SyncResult> synchronizeCommunityTopics() {
        return new ResponseBean<>(communitySyncService.drcSync());
    }

    // endTime 和 startTime 這兩個參數是用來指定要刪除的時間範圍
    // 輸入格式: Unix timestamp in milliseconds (e.g., 1724832000000) 共 13 位數字
    // DB 是local time 的時間
    @Override
    public ResponseBean<SyncResult> deleteSynchronizedCommunityTopics(
            @RequestParam Long startTime,
            @RequestParam Long endTime) {

        Date startDateTime = new Date(startTime);
        Date endDateTime = new Date(endTime);

        SyncResult result = communitySyncService.deleteSynchronizedCommunityTopics(startDateTime, endDateTime);
        return new ResponseBean<>(result);
    }
}
