package com.delta.dms.community.dao;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.delta.dms.community.swagger.model.DeviceInfo;

public interface DeviceDao {

  /**
   * Delete bad device token
   *
   * @param token Device token
   * @return Numbers of token deleted
   */
  public int deleteBadDeviceToken(@Param("token") String token);

  /**
   * Register device
   *
   * @param deviceInfo
   * @return Numbers of device inserted
   */
  public int registerDevice(DeviceInfo deviceInfo);

  /**
   * Get the device information list of the user
   *
   * @param userId User id
   * @return The device information list
   */
  public List<DeviceInfo> getDeviceTokenListOfUser(@Param("userId") String userId);
}
