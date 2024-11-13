package com.delta.dms.community.dao;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.dao.entity.UserInfo;

public interface UserDao {
  /**
   * Get User's information by id
   *
   * @param userId User id
   * @return User's information
   */
  public UserInfo getUserById(@Param("userId") String userId);

  /**
   * Get User's status by id
   *
   * @param userId User id
   * @return User's status
   */
  public int getUserStatus(@Param("userId") String userId);

  /**
   * Get Users' information by id list
   *
   * @param userIdList User id list
   * @return Users' information
   */
  public List<UserInfo> getUserByIds(@Param("idList") List<String> userIdList);
}
