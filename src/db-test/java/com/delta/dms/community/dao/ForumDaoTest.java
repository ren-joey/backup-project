package com.delta.dms.community.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
import com.delta.dms.community.dao.entity.ForumInfo;
import com.delta.dms.community.swagger.model.CommunityStatus;
import com.delta.dms.community.swagger.model.ForumType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao-test.xml", "classpath:spring/spring-service-test.xml"})
public class ForumDaoTest {
  @Autowired
  private ForumDao forumDao;

  private final Integer communityId = 1;
  private final Integer forumId = 1;
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
    String insertTableSql = "INSERT ignore INTO dms_community.forums"
        + "(community_id, forum_name, forum_desc, forum_type, forum_status, forum_create_user_id, forum_create_time, "
        + "forum_last_modified_user_id, forum_last_modified_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    PreparedStatement prepareStatement = connection.prepareStatement(insertTableSql);
    prepareStatement.setInt(1, communityId);
    prepareStatement.setString(2, name);
    prepareStatement.setString(3, desc);
    prepareStatement.setString(4, ForumType.PUBLIC.toString());
    prepareStatement.setString(5, CommunityStatus.OPEN.toString());
    prepareStatement.setString(6, userId);
    prepareStatement.setLong(7, milliseconds);
    prepareStatement.setString(8, userId);
    prepareStatement.setLong(9, milliseconds);
    prepareStatement.execute();
  }

  @After
  public void tearDown() throws Exception {
    PreparedStatement prepareStatement = connection.prepareStatement("TRUNCATE TABLE dms_community.forums");
    prepareStatement.execute();
  }

  @Test
  public void test_Add_Forum() throws Exception {
    ForumInfo forumInfo = new ForumInfo();
    ForumInfo expectedForumInfo = new ForumInfo();
    add_Forum(forumInfo);
    Assert.assertTrue(forumInfo.getForumId() > 0);

    expectedForumInfo = forumDao.getForumById(forumInfo.getForumId(), "enUs");
    Assert.assertEquals(expectedForumInfo.getCommunityId(), forumInfo.getCommunityId());
    Assert.assertEquals(expectedForumInfo.getForumStatus(), forumInfo.getForumStatus());
    Assert.assertEquals(expectedForumInfo.getForumType(), forumInfo.getForumType());
  }

  @Test
  public void test_Update_Forum() throws Exception {
    ForumInfo forumInfo = new ForumInfo();
    Date now = new Date();
    long millisecondsUpdate = now.getTime();

    forumInfo.setForumId(forumId);
    forumInfo.setForumName(nameUpdate);
    forumInfo.setForumDesc(descUpdate);
    forumInfo.setForumType(ForumType.PUBLIC.toString());
    forumInfo.setForumStatus(CommunityStatus.OPEN.toString());
    forumInfo.setForumModifiedUserId(userIdUpdate);
    forumInfo.setForumModifiedTime(millisecondsUpdate);
    forumInfo.setForumLastModifiedUserId(userIdUpdate);
    forumInfo.setForumLastModifiedTime(millisecondsUpdate);
    Integer effectNumber = forumDao.update(forumInfo);
    Assert.assertSame(1, effectNumber);

    ForumInfo expectedForumInfo = forumDao.getForumById(forumId, "enUs");
    Assert.assertEquals(expectedForumInfo.getForumName(), forumInfo.getForumName());
    Assert.assertEquals(expectedForumInfo.getForumStatus(), forumInfo.getForumStatus());
    Assert.assertEquals(expectedForumInfo.getForumType(), forumInfo.getForumType());
  }

  @Test
  public void test_GetForumById() throws Exception {
    ForumInfo forumInfo = forumDao.getForumById(forumId, "enUs");
    Assert.assertEquals(userId, forumInfo.getForumCreateUserId());
  }

  @Test
  public void test_GetAllByCommunityId() throws Exception {
    List<ForumInfo> forumInfos = forumDao.getAllByCommunityIdAndStatus(communityId, CommunityStatus.OPEN.toString());
    Assert.assertTrue(forumInfos.size() > 0);
  }

  public void add_Forum(ForumInfo forumInfo) {
    Date now = new Date();
    long milliseconds = now.getTime();

    forumInfo.setCommunityId(communityId);
    forumInfo.setForumName(name);
    forumInfo.setForumDesc(desc);
    forumInfo.setForumType(ForumType.PUBLIC.toString());
    forumInfo.setForumStatus(CommunityStatus.OPEN.toString());
    forumInfo.setForumCreateUserId(userId);
    forumInfo.setForumCreateTime(milliseconds);
    forumInfo.setForumLastModifiedUserId(userIdUpdate);
    forumInfo.setForumLastModifiedTime(milliseconds);
    forumDao.add(forumInfo);
  }
}
