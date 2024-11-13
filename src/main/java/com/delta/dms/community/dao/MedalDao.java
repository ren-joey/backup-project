package com.delta.dms.community.dao;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.dao.entity.Medal;
import com.delta.dms.community.enums.MedalIdType;
import com.delta.dms.community.swagger.model.AwardDto;

public interface MedalDao {

  void setCommunityActivateMedal(
      @Param("communityId") int communityId, @Param("medalId") Integer medalId);

  default Map<String, List<Medal>> getMedals(MedalIdType medalIdType, Set<String> ids) {
    return isEmpty(ids)
        ? emptyMap()
        : selectMedals(medalIdType, ids)
            .stream()
            .collect(groupingBy(Medal::getTargetId, LinkedHashMap::new, toList()));
  }

  List<Medal> selectMedals(
      @Param("medalIdType") MedalIdType medalIdType, @Param("ids") Set<String> ids);

  List<AwardDto> getCommunityAwards(@Param("communityId") int communityId);
  
  List<String> getCertificationByIdAndType(@Param("id") String id, @Param("medalType") String medalType);
}
