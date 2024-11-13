package com.delta.dms.community.event;

import org.springframework.context.ApplicationEvent;
import com.delta.dms.community.swagger.model.ForumData;

public class CommunityCreatingEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;

  private ForumData forumData;

  public CommunityCreatingEvent(Object source, ForumData forumData) {
    super(source);
    this.forumData = forumData;
  }

  public ForumData getForumData() {
    return forumData;
  }
}
