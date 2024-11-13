package com.delta.dms.community.service.view;

import static com.delta.dms.community.service.view.CommunityMemberTableViewRender.TableViewRenderType.DERIVATIVE;
import static com.delta.dms.community.service.view.CommunityMemberTableViewRender.TableViewRenderType.INDEPENDENT;

import com.delta.dms.community.service.view.CommunityMemberTableViewRender.TableViewRenderType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CommunityMemberTableViewRenderFactory {

  private final List<CommunityMemberTableViewRender> renders;
  private static final Map<TableViewRenderType, CommunityMemberTableViewRender> renderMap = new HashMap<>();

  @PostConstruct
  public void init() {
    renders.forEach(render -> renderMap.put(render.renderType(), render));
  }

  public CommunityMemberTableViewRender getRender(TableViewRenderType renderType) {
    return renderMap.get(renderType);
  }

  /**
   * get render if community is derivative
   * @param isDerivativeCommunity community is derivative or not
   * @return table view render
   */
  public CommunityMemberTableViewRender getRender(boolean isDerivativeCommunity) {
    return getRender(isDerivativeCommunity ? DERIVATIVE : INDEPENDENT);
  }
}
