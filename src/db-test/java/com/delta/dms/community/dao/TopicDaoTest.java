package com.delta.dms.community.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.delta.dms.community.dao.entity.TopicInfo;
import com.delta.dms.community.swagger.model.CommunityStatus;
import com.delta.dms.community.swagger.model.TopicState;
import com.delta.dms.community.swagger.model.TopicStatus;
import com.delta.dms.community.swagger.model.TopicType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao-test.xml", "classpath:spring/spring-service-test.xml"})
public class TopicDaoTest {
  @Autowired
  private TopicDao topicDao;

  private final Integer forumId = 1;
  private final Integer topicId = 1;
  private final String title = "title1";
  private final String userId = "u000001";
  private final String text = "text1";
  private final String titleUpdate = "title2";
  private final String userIdUpdate = "u000002";
  private final String textUpdate = "text2";

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
    String insertTableSql = "INSERT ignore INTO dms_community.topics"
        + "(forum_id, topic_title, topic_status, topic_create_user_id, topic_create_time, topic_last_modified_user_id, topic_last_modified_time ) "
        + "VALUES (?, ?, ?, ?, ?, ?,? )";
    PreparedStatement prepareStatement = connection.prepareStatement(insertTableSql);
    prepareStatement.setInt(1, forumId);
    prepareStatement.setString(2, title);
    prepareStatement.setString(3, TopicStatus.OPEN.toString());
    prepareStatement.setString(4, userId);
    prepareStatement.setLong(5, milliseconds);
    prepareStatement.setString(6, userId);
    prepareStatement.setLong(7, milliseconds);
    prepareStatement.execute();

    insertTableSql = "INSERT ignore INTO dms_community.topics_text" + "(topic_id, topic_text) VALUES " + "(?, ?)";
    prepareStatement = connection.prepareStatement(insertTableSql);
    prepareStatement.setInt(1, topicId);
    prepareStatement.setString(2, text);
    prepareStatement.execute();
  }

  @After
  public void tearDown() throws Exception {
    PreparedStatement prepareStatement = connection.prepareStatement("TRUNCATE TABLE dms_community.topics");
    prepareStatement.execute();

    prepareStatement = connection.prepareStatement("TRUNCATE TABLE dms_community.topics_text");
    prepareStatement.execute();
  }

  @Test
  public void test_Add_Topic() throws Exception {
    TopicInfo topicInfo = new TopicInfo();
    TopicInfo expectedTopicInfo = new TopicInfo();
    add_Topic(topicInfo);
    Assert.assertTrue(topicInfo.getTopicId() > 0);

    expectedTopicInfo = topicDao.getTopicById(topicId);
    Assert.assertEquals(expectedTopicInfo.getForumId(), topicInfo.getForumId());
    Assert.assertEquals(expectedTopicInfo.getTopicStatus(), topicInfo.getTopicStatus());
    Assert.assertEquals(expectedTopicInfo.getTopicTitle(), topicInfo.getTopicTitle());
    Assert.assertEquals(expectedTopicInfo.getTopicText(), topicInfo.getTopicText());
  }

  @Test
  public void test_Update() throws Exception {
    TopicInfo topicInfo = new TopicInfo();
    Date now = new Date();
    long millisecondsUpdate = now.getTime();

    topicInfo.setTopicId(topicId);
    topicInfo.setTopicTitle(titleUpdate);
    topicInfo.setTopicStatus(CommunityStatus.OPEN.toString());
    topicInfo.setTopicModifiedUserId(userIdUpdate);
    topicInfo.setTopicModifiedTime(millisecondsUpdate);
    topicInfo.setTopicLastModifiedUserId(userIdUpdate);
    topicInfo.setTopicLastModifiedTime(millisecondsUpdate);
    topicInfo.setTopicText(textUpdate);

    Integer effectNumber = topicDao.updateInfo(topicInfo);
    Assert.assertSame(1, effectNumber);
    effectNumber = topicDao.updateText(topicInfo);
    Assert.assertSame(1, effectNumber);

    TopicInfo expectedTopicInfo = topicDao.getTopicById(topicId);
    Assert.assertEquals(expectedTopicInfo.getTopicStatus(), topicInfo.getTopicStatus());
    Assert.assertEquals(expectedTopicInfo.getTopicTitle(), topicInfo.getTopicTitle());
    Assert.assertEquals(expectedTopicInfo.getTopicText(), topicInfo.getTopicText());
  }

  @Test
  public void test_GetTopicById() throws Exception {
    TopicInfo topicInfo = topicDao.getTopicById(topicId);
    Assert.assertEquals(userId, topicInfo.getTopicCreateUserId());
  }

  @Test
  public void test_GetAllByForumId() throws Exception {
    List<TopicInfo> topicInfos = topicDao.getAllByForumIdAndStatus(forumId, TopicStatus.OPEN.toString());
    Assert.assertTrue(topicInfos.size() > 0);
  }

  public void add_Topic(TopicInfo topicInfo) {
    Date now = new Date();
    long milliseconds = now.getTime();

    topicInfo.setForumId(forumId);
    topicInfo.setTopicTitle(title);
    topicInfo.setTopicStatus(CommunityStatus.OPEN.toString());
    topicInfo.setTopicState(TopicState.UNCONCLUDED.toString());
    topicInfo.setTopicType(TopicType.GENERAL.toString());
    topicInfo.setTopicCreateUserId(userId);
    topicInfo.setTopicCreateTime(milliseconds);
    topicInfo.setTopicLastModifiedUserId(userId);
    topicInfo.setTopicLastModifiedTime(milliseconds);
    topicInfo.setTopicText(text);
    topicDao.addInfo(topicInfo);
    topicDao.addText(topicInfo);
  }
}
