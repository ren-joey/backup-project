package com.delta.dms.community.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang.math.NumberUtils.INTEGER_ZERO;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.delta.dms.community.dao.MedalDao;
import com.delta.dms.community.dao.entity.Medal;
import com.delta.dms.community.enums.MedalIdType;
import com.delta.dms.community.swagger.model.AwardDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MedalService {

  private final MedalDao medalDao;

  public Map<String, List<Medal>> getMedals(MedalIdType medalIdType, Set<String> ids) {
    return medalDao.getMedals(medalIdType, ids);
  }

  public List<Medal> getMedals(MedalIdType medalIdType, String id) {
    return getMedals(medalIdType, singleton(id)).getOrDefault(id, emptyList());
  }

  public List<AwardDto> getCommunityAwards(int communityId) {
    return medalDao.getCommunityAwards(communityId);
  }

  public void setCommunityActivateMedal(int communityId, Integer medalId) {
    if (nonNull(medalId)
        && (medalId.equals(INTEGER_ZERO)
            || getMedals(MedalIdType.COMMUNITY, String.valueOf(communityId))
                .stream()
                .filter(medal -> !medal.isDisabled())
                .anyMatch(medal -> medalId.equals(medal.getId())))) {
      medalDao.setCommunityActivateMedal(communityId, medalId);
    }
  }
}
