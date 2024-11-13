package com.delta.dms.community.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.delta.dms.community.dao.entity.CommunityInfo;
import com.delta.dms.community.swagger.model.CommunityCategory;
import com.delta.dms.community.swagger.model.CommunityStatus;
import com.delta.dms.community.swagger.model.CommunityType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao-test.xml", "classpath:spring/spring-service-test.xml"})
public class CommunityDaoTest {
  @Autowired
  private CommunityDao communityDao;

  private final Integer communityId = 1;
  private final String name = "comm1";
  private final String desc = "desc1";
  private final String userId = "u000001";
  private final String nameUpdate = "comm2";
  private final String descUpdate = "desc2";
  private final String userIdUpdate = "u000002";

  private static Connection connection;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    MariaDB4jManager.INSTANCE.create();
    connection = MariaDB4jManager.INSTANCE.getConnection();
  }

  @Before
  public void setUp() throws Exception {
    Date now = new Date();
    long milliseconds = now.getTime();
    String insertTableSql = "INSERT ignore INTO dms_community.communities"
        + "(community_name, community_desc, community_type, community_status, community_create_user_id, community_create_time, "
        + "community_last_modified_user_id, community_last_modified_time, community_category) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    PreparedStatement prepareStatement = connection.prepareStatement(insertTableSql);
    prepareStatement.setString(1, name);
    prepareStatement.setString(2, desc);
    prepareStatement.setString(3, CommunityType.PUBLIC.toString());
    prepareStatement.setString(4, CommunityStatus.OPEN.toString());
    prepareStatement.setString(5, userId);
    prepareStatement.setLong(6, milliseconds);
    prepareStatement.setString(7, userId);
    prepareStatement.setLong(8, milliseconds);
    prepareStatement.setString(9, CommunityCategory.GENERAL.toString());
    prepareStatement.execute();
  }

  @After
  public void tearDown() throws Exception {
    PreparedStatement prepareStatement = connection.prepareStatement("TRUNCATE TABLE dms_community.communities");
    prepareStatement.execute();
  }

  @Test
  public void test_Add_Community() throws Exception {
    CommunityInfo communityInfo = new CommunityInfo();
    CommunityInfo expectedCommunityInfo = new CommunityInfo();
    add_Community(communityInfo);
    Assert.assertTrue(communityInfo.getCommunityId() > 0);

    expectedCommunityInfo = communityDao.getCommunityById(Collections.singletonList(communityInfo.getCommunityId())).get(0);
    Assert.assertEquals(expectedCommunityInfo.getCommunityCategory(), communityInfo.getCommunityCategory());
    Assert.assertEquals(expectedCommunityInfo.getCommunityStatus(), communityInfo.getCommunityStatus());
    Assert.assertEquals(expectedCommunityInfo.getCommunityCreateUserId(), communityInfo.getCommunityCreateUserId());
  }

  @Test
  public void test_Update_Community() throws Exception {
    CommunityInfo communityInfo = new CommunityInfo();
    Date now = new Date();
    long millisecondsUpdate = now.getTime();
    communityInfo.setCommunityId(communityId);
    communityInfo.setCommunityName(nameUpdate);
    communityInfo.setCommunityDesc(descUpdate);
    communityInfo.setCommunityType(CommunityType.PUBLIC.toString());
    communityInfo.setCommunityStatus(CommunityStatus.OPEN.toString());
    communityInfo.setCommunityModifiedUserId(userIdUpdate);
    communityInfo.setCommunityModifiedTime(millisecondsUpdate);
    communityInfo.setCommunityLastModifiedUserId(userId);
    communityInfo.setCommunityLastModifiedTime(millisecondsUpdate);
    Integer effectNumber = communityDao.update(communityInfo);
    Assert.assertSame(1, effectNumber);

    CommunityInfo expectedCommunityInfo = communityDao.getCommunityById(Collections.singletonList(communityInfo.getCommunityId())).get(0);
    Assert.assertEquals(expectedCommunityInfo.getCommunityName(), communityInfo.getCommunityName());
    Assert.assertEquals(expectedCommunityInfo.getCommunityStatus(), communityInfo.getCommunityStatus());
    Assert.assertEquals(expectedCommunityInfo.getCommunityType(), communityInfo.getCommunityType());
  }

  @Test
  public void test_Delete_Community() throws Exception {
    CommunityInfo communityInfo = new CommunityInfo();
    Date now = new Date();
    long millisecondsUpdate = now.getTime();
    communityInfo.setCommunityId(communityId);
    communityInfo.setCommunityStatus(CommunityStatus.DELETE.toString());
    communityInfo.setCommunityDeleteUserId(userIdUpdate);
    communityInfo.setCommunityDeleteTime(millisecondsUpdate);
    communityInfo.setCommunityLastModifiedUserId(userId);
    communityInfo.setCommunityLastModifiedTime(millisecondsUpdate);
    Integer effectNumber = communityDao.delete(communityInfo);
    Assert.assertSame(1, effectNumber);
  }

  @Test
  public void test_GetCommunityById() throws Exception {
    CommunityInfo communityInfo = communityDao.getCommunityById(Collections.singletonList(communityId)).get(0);
    Assert.assertEquals(userId, communityInfo.getCommunityCreateUserId());
  }

  @Test
  public void test_GetAll() throws Exception {
    List<CommunityInfo> communityInfos = communityDao.getAll();
    Assert.assertTrue(communityInfos.size() > 0);
  }

  public void add_Community(CommunityInfo communityInfo) {
    Date now = new Date();
    long milliseconds = now.getTime();

    communityInfo.setCommunityName(name);
    communityInfo.setCommunityDesc(desc);
    communityInfo.setCommunityType(CommunityType.PUBLIC.toString());
    communityInfo.setCommunityStatus(CommunityStatus.OPEN.toString());
    communityInfo.setCommunityCreateUserId(userId);
    communityInfo.setCommunityCreateTime(milliseconds);
    communityInfo.setCommunityLastModifiedUserId(userId);
    communityInfo.setCommunityLastModifiedTime(milliseconds);
    communityInfo.setCommunityCategory(CommunityCategory.GENERAL.toString());
    communityDao.add(communityInfo);
  }
}
