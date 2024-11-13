package com.delta.dms.community.service;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import com.delta.dms.community.adapter.UserGroupAdapter;
import com.delta.dms.community.adapter.entity.OrgGroup;
import com.delta.dms.community.adapter.entity.OrgGroup.OrgGroupType;
import com.delta.dms.community.adapter.entity.OrgProfile;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrgGroupExtensionService {
  public static final String SEARCH_LEFT_PARENTHESIS = "(";
  public static final String SEARCH_RIGHT_PARENTHESIS = ")";
  private final UserGroupAdapter userGroupAdapter;

  public String getOrgName(OrgGroup orgGroup) {
    if (OrgGroupType.ProjectGroup.equals(orgGroup.getType())) {
      return orgGroup.getName();
    } else {
      OrgProfile orgProfile = userGroupAdapter.getOrgProfileByGid(orgGroup.getId());
      if (Objects.isNull(orgProfile)) {
        return orgGroup.getName();
      } else {
        String orgName =
            Stream.of(orgProfile.getOrgName(), orgProfile.getOrgEName())
                .filter(Objects::nonNull)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.joining(StringUtils.SPACE));
        String orgAbbr =
            Optional.ofNullable(orgProfile.getOrgAbbr())
                .filter(Objects::nonNull)
                .filter(abbr -> !abbr.isEmpty())
                .map(
                    abbr ->
                        Stream.of(abbr)
                            .collect(
                                Collectors.joining(
                                    StringUtils.EMPTY,
                                    SEARCH_LEFT_PARENTHESIS,
                                    SEARCH_RIGHT_PARENTHESIS)))
                .orElseGet(() -> StringUtils.EMPTY);
        return Stream.of(orgName, orgAbbr)
            .filter(name -> !name.isEmpty())
            .collect(Collectors.joining(StringUtils.SPACE));
      }
    }
  }
}
