package com.delta.dms.community.event;

import java.util.List;
import org.springframework.context.ApplicationEvent;
import com.delta.dms.community.swagger.model.ForumType;

public class FileUploadingEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;
  private String fileId;
  private List<String> author;
  private int communityId;
  private int forumId;
  private ForumType forumType;
  private boolean updateDtuMapping;
  private String videoLanguage;

  public FileUploadingEvent(
      Object source,
      String fileId,
      List<String> author,
      int communityId,
      int forumId,
      ForumType forumType,
      boolean updateDtuMapping,
      String videoLanguage) {
    super(source);
    this.fileId = fileId;
    this.author = author;
    this.communityId = communityId;
    this.forumId = forumId;
    this.forumType = forumType;
    this.updateDtuMapping = updateDtuMapping;
    this.videoLanguage = videoLanguage;
  }

  public String getFileId() {
    return fileId;
  }

  public List<String> getAuthor() {
    return author;
  }

  public int getCommunityId() {
    return communityId;
  }

  public int getForumId() {
    return forumId;
  }

  public ForumType getForumType() {
    return forumType;
  }

  public boolean needUpdateDtuMapping() {
    return updateDtuMapping;
  }

  public String getVideoLanguage() {
    return videoLanguage;
  }
}
