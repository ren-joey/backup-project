package com.delta.dms.community.service.privilege.topic;

import static com.delta.dms.community.utils.Constants.ERR_INVALID_PARAM;
import static java.util.Optional.ofNullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import com.delta.dms.community.swagger.model.TopicOperation;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TopicPrivilegeService {

  private static final Map<TopicOperation, BasePrivilegeValidator> validatorMap =
      new EnumMap<>(TopicOperation.class);

  private final List<BasePrivilegeValidator> validators;

  @PostConstruct
  public void init() {
    validators.forEach(validator -> validatorMap.put(validator.getTopicOperation(), validator));
  }

  public void validatePrivilege(TopicOperation operation, int topicId) {
    getValidator(operation).validatePrivilege(topicId);
  }

  private BasePrivilegeValidator getValidator(TopicOperation operation) {
    return ofNullable(validatorMap.get(operation))
        .orElseThrow(() -> new IllegalArgumentException(ERR_INVALID_PARAM));
  }
}
