package com.delta.dms.community.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.delta.dms.community.dao.DeviceDao;
import com.delta.dms.community.swagger.model.DeviceInfo;

@Service
public class DeviceService {

  private DeviceDao deviceDao;

  @Autowired
  public DeviceService(DeviceDao deviceDao) {
    this.deviceDao = deviceDao;
  }

  public void deleteBadDeviceToken(String token) {
    deviceDao.deleteBadDeviceToken(token);
  }

  public void registerDevice(DeviceInfo deviceInfo) {
    deviceDao.registerDevice(deviceInfo);
  }

  public List<DeviceInfo> getDeviceTokenListOfUser(String userId) {
    return deviceDao.getDeviceTokenListOfUser(userId);
  }
}
