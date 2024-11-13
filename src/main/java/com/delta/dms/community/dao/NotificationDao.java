package com.delta.dms.community.dao;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.swagger.model.Notification;

public interface NotificationDao {

  /**
   * Get the notifications of the user
   *
   * @param userId User id
   * @return The result list
   */
  public List<Notification> getNotificationOfUser(
      @Param("userId") String userId, @Param("offset") int offset, @Param("limit") int limit);

  /**
   * Store the notification
   *
   * @param notification Notification information
   * @return The numbers of row added
   */
  public int storeNotification(Notification notification);

  /**
   * Set the notification status to read
   *
   * @param id Notification id
   * @return The numbers of row updated
   */
  public int readNotification(@Param("id") int id);

  /**
   * Get the information of the notification
   *
   * @param id Notification
   * @return The notification
   */
  public Notification getNotificationById(@Param("id") int id);

  /**
   * Store unreviewed notification
   *
   * @param id Notification id
   * @param applicationId
   * @return The numbers of row inserted
   */
  public int storeUnreviewedNotification(
      @Param("id") List<Integer> id, @Param("applicationId") int applicationId);

  /**
   * Get the application id by notification type, applicant id and application time
   *
   * @param type Notification type
   * @param applicantId Applicant id
   * @param time Application time
   * @return The application id
   */
  public int getApplicationIdByTypeAndTime(
      @Param("type") String type,
      @Param("applicantId") String applicantId,
      @Param("applicationTime") long time);

  /**
   * Renew user's last access time of accessing notification list
   *
   * @param userId User id
   * @param time Access time
   * @return Numbers of row inserted or updated
   */
  public int renewAccessTime(@Param("userId") String userId, @Param("time") long time);

  /**
   * Count the new notification of the user
   *
   * @param userId User id
   * @return Numbers of the notification
   */
  public int countNotification(@Param("userId") String userId);
}
