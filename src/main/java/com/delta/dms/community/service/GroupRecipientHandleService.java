package com.delta.dms.community.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import com.delta.dms.community.service.member.CollectMemberService;
import com.delta.dms.community.swagger.model.SimpleGroupWithUsers;
import com.delta.dms.community.swagger.model.User;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class GroupRecipientHandleService {
  private final CollectMemberService collectMemberService;

  protected List<String> getOrgRecipient(List<SimpleGroupWithUsers> orgMembers) {
    List<String> orgRecipient = new ArrayList<>();
    Optional.ofNullable(orgMembers)
        .filter(list -> !CollectionUtils.isEmpty(list))
        .ifPresent(
            list ->
                list.forEach(
                    item -> {
                      List<String> members =
                          Optional.ofNullable(item.getUsers())
                              .orElseGet(ArrayList::new)
                              .parallelStream()
                              .map(User::getId)
                              .filter(StringUtils::isNoneBlank)
                              .collect(Collectors.toList());
                      if (CollectionUtils.isEmpty(members)) {
                        members =
                            Optional.ofNullable(
                                    collectMemberService.getGeneralMembersByOrgId(item.getValue()))
                                .orElseGet(ArrayList::new)
                                .parallelStream()
                                .map(User::getId)
                                .filter(StringUtils::isNoneBlank)
                                .collect(Collectors.toList());
                      }
                      orgRecipient.addAll(members);
                    }));

    return orgRecipient;
  }
}
