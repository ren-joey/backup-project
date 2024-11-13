package com.delta.dms.community.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.delta.dms.community.dao.entity.ReplyInfo;
import com.delta.dms.community.swagger.model.CommunityStatus;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao-test.xml", "classpath:spring/spring-service-test.xml"})
public class ReplyDaoTest {
  @Autowired
  private ReplyDao replyDao;

  private final Integer replyId = 1;
  private final Integer topicId = 1;
  private final Integer forumId = 1;
  private final String userId = "u000001";
  private final String text = "text1";
  private final String userIdUpdate = "u000002";
  private final String textUpdate = "text2";
  private final int replyIndex = 1;
  private final String replyRespondee = "u000001";
  private final String respondee = "user";

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
    String insertTableSql = "INSERT ignore INTO dms_community.replies"
        + "(forum_id, follow_topic_id, reply_status, reply_create_user_id, reply_create_time, reply_index, reply_respondee) VALUES "
        + "(?, ?, ?, ?, ?, ?, ?)";
    PreparedStatement prepareStatement = connection.prepareStatement(insertTableSql);
    prepareStatement.setInt(1, forumId);
    prepareStatement.setInt(2, topicId);
    prepareStatement.setString(3, CommunityStatus.OPEN.toString());
    prepareStatement.setString(4, userId);
    prepareStatement.setLong(5, milliseconds);
    prepareStatement.setInt(6, replyIndex);
    prepareStatement.setString(7, replyRespondee);
    prepareStatement.execute();

    insertTableSql = "INSERT ignore INTO dms_community.replies_text" + "(reply_id, reply_text) VALUES " + "(?, ?)";
    prepareStatement = connection.prepareStatement(insertTableSql);
    prepareStatement.setInt(1, replyId);
    prepareStatement.setString(2, text);
    prepareStatement.execute();
  }

  @After
  public void tearDown() throws Exception {
    PreparedStatement prepareStatement = connection.prepareStatement("TRUNCATE TABLE dms_community.replies");
    prepareStatement.execute();

    prepareStatement = connection.prepareStatement("TRUNCATE TABLE dms_community.replies_text");
    prepareStatement.execute();
  }

  @Test
  public void test_Add_Reply() throws Exception {
    ReplyInfo replyInfo = new ReplyInfo();
    ReplyInfo expectedReplyInfo = new ReplyInfo();
    add_Reply(replyInfo);
    Assert.assertTrue(replyInfo.getReplyId() > 0);

    expectedReplyInfo = replyDao.getReplyById(replyId);
    Assert.assertEquals(expectedReplyInfo.getForumId(), replyInfo.getForumId());
    Assert.assertEquals(expectedReplyInfo.getReplyStatus(), replyInfo.getReplyStatus());
    Assert.assertEquals(expectedReplyInfo.getReplyText(), replyInfo.getReplyText());
  }

  @Test
  public void test_Update_Reply() throws Exception {
    ReplyInfo replyInfo = new ReplyInfo();
    Date now = new Date();
    long millisecondsUpdate = now.getTime();

    replyInfo.setReplyId(replyId);
    replyInfo.setFollowTopicId(topicId);
    replyInfo.setReplyStatus(CommunityStatus.OPEN.toString());
    replyInfo.setReplyModifiedUserId(userIdUpdate);
    replyInfo.setReplyModifiedTime(millisecondsUpdate);
    replyInfo.setReplyText(textUpdate);
    replyInfo.setReplyRespondee("u000001");
    Integer effectNumber = replyDao.updateInfo(replyInfo);
    Assert.assertSame(1, effectNumber);
    effectNumber = replyDao.updateText(replyInfo);
    Assert.assertSame(1, effectNumber);

    ReplyInfo expectedReplyInfo = replyDao.getReplyById(replyId);
    Assert.assertEquals(expectedReplyInfo.getReplyStatus(), replyInfo.getReplyStatus());
    Assert.assertEquals(expectedReplyInfo.getReplyText(), replyInfo.getReplyText());
    Assert.assertEquals(expectedReplyInfo.getReplyRespondee(), replyInfo.getReplyRespondee());
  }

  @Test
  public void test_GetReplyById() throws Exception {
    ReplyInfo replyInfo = replyDao.getReplyById(replyId);
    Assert.assertEquals(userId, replyInfo.getReplyCreateUserId());
  }

  public void add_Reply(ReplyInfo replyInfo) {
    Date now = new Date();
    long milliseconds = now.getTime();

    replyInfo.setForumId(forumId);
    replyInfo.setFollowTopicId(topicId);
    replyInfo.setReplyStatus(CommunityStatus.OPEN.toString());
    replyInfo.setReplyCreateUserId(userId);
    replyInfo.setReplyCreateTime(milliseconds);
    replyInfo.setReplyText(text);
    replyInfo.setReplyIndex(replyIndex);
    replyInfo.setReplyRespondee(respondee);
    replyDao.addInfo(replyInfo);
    replyDao.addText(replyInfo);
  }
}
