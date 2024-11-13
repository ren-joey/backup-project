package com.delta.dms.community.service.privilege.reply;

import static com.delta.dms.community.utils.Constants.ERR_INVALID_PARAM;
import static java.util.Optional.ofNullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import com.delta.dms.community.swagger.model.ReplyOperation;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ReplyPrivilegeService {

  private static final Map<ReplyOperation, BasePrivilegeValidator> validatorMap =
      new EnumMap<>(ReplyOperation.class);

  private final List<BasePrivilegeValidator> validators;

  @PostConstruct
  public void init() {
    validators.forEach(validator -> validatorMap.put(validator.getReplyOperation(), validator));
  }

  public void validatePrivilege(ReplyOperation operation, int replyId) {
    getValidator(operation).validatePrivilege(replyId);
  }

  private BasePrivilegeValidator getValidator(ReplyOperation operation) {
    return ofNullable(validatorMap.get(operation))
        .orElseThrow(() -> new IllegalArgumentException(ERR_INVALID_PARAM));
  }
}
