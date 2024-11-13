package com.delta.dms.community.service.privilege.topic;

import com.delta.dms.community.dao.TopicDao;
import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.service.CommunityService;
import com.delta.dms.community.service.ForumService;
import com.delta.dms.community.service.PrivilegeService;
import com.delta.dms.community.swagger.model.Operation;
import com.delta.dms.community.swagger.model.PermissionObject;
import com.delta.dms.community.swagger.model.TopicOperation;
import org.springframework.stereotype.Component;

import static com.delta.dms.community.swagger.model.Operation.UPDATE;
import static com.delta.dms.community.swagger.model.PermissionObject.ACTIVETOPIC;
import static com.delta.dms.community.swagger.model.TopicOperation.REPLYREPORTGENERATE;
import static com.delta.dms.community.utils.I18nConstants.MSG_CANNOT_GENERATE_TOPIC_REPLY_REPORT;

@Component
public class GenerateReplyReportValidator extends BasePrivilegeValidator {
    public GenerateReplyReportValidator(
            CommunityService communityService,
            ForumService forumService,
            PrivilegeService privilegeService,
            TopicDao topicDao) {
        super(communityService, forumService, privilegeService, topicDao);
    }

    @Override
    public TopicOperation getTopicOperation() {
        return REPLYREPORTGENERATE;
    }

    @Override
    Operation getPrivilegeOperation() { return UPDATE; }

    @Override
    String getErrorMessage() { return MSG_CANNOT_GENERATE_TOPIC_REPLY_REPORT; }

    @Override
    PermissionObject getPermissionObject(CommunityInfo community, ForumInfo forum) {
        return ACTIVETOPIC;
    }

    @Override
    boolean allowCreatorOperate() {
        return true;
    }

    @Override
    void validateExtraTopicStatus(TopicInfo topic) {
    }

}
