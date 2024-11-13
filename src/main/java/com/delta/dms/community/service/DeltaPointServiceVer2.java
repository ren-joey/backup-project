package com.delta.dms.community.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.delta.dms.community.adapter.UserGroupAdapter;
import com.delta.dms.community.swagger.model.DeltaPointInfo;
import com.delta.dms.community.swagger.model.DeltaPointLineChart;
import com.delta.dms.community.swagger.model.DeltaPointLineChartDetail;
import com.delta.dms.community.swagger.model.DeltaPointType;
import com.delta.dms.community.swagger.model.LineChartPoint;

@Service
@Transactional
public class DeltaPointServiceVer2 {
  private static final String EMERALD = "1";
  private static final String SAPPHIRE = "2";
  private static final String RUBY = "3";
  private static final String DIAMOND = "4";

  private UserGroupAdapter userGroupAdapter;

  @Autowired
  public DeltaPointServiceVer2(UserGroupAdapter userGroupAdapter) {
    this.userGroupAdapter = userGroupAdapter;
  }

  public List<DeltaPointInfo> getDeltaPointByTimePeriod(
      String userId, long stime, long etime, boolean detail) {
    return userGroupAdapter.getDeltaPoint(userId, stime, etime, detail);
  }

  public DeltaPointLineChart getLineChartDP(String userId) {
    LocalDate today = LocalDate.now();
    LocalDate thismonth = today.withDayOfMonth(1);
    LocalDate premonth = thismonth.minusMonths(1);
    LocalDate pre2month = thismonth.minusMonths(2);

    final List<DeltaPointInfo> one =
        getDeltaPointByTimePeriod(
            userId, toTimestamp(pre2month), (toTimestamp(premonth) - 1), false);
    final List<DeltaPointInfo> two =
        getDeltaPointByTimePeriod(
            userId, toTimestamp(premonth), (toTimestamp(thismonth) - 1), false);
    final List<DeltaPointInfo> three =
        getDeltaPointByTimePeriod(userId, toTimestamp(thismonth), toTimestamp(today), false);

    final DeltaPointLineChart lineChart = new DeltaPointLineChart();
    DeltaPointLineChartDetail emeraldDetail = new DeltaPointLineChartDetail();
    emeraldDetail.setLevelName(EMERALD);
    DeltaPointLineChartDetail sapphireDetail = new DeltaPointLineChartDetail();
    sapphireDetail.setLevelName(SAPPHIRE);
    DeltaPointLineChartDetail rubyDetail = new DeltaPointLineChartDetail();
    rubyDetail.setLevelName(RUBY);
    DeltaPointLineChartDetail diamondDetail = new DeltaPointLineChartDetail();
    diamondDetail.setLevelName(DIAMOND);

    addPoint(one, 1, emeraldDetail, sapphireDetail, rubyDetail, diamondDetail);
    addPoint(two, 2, emeraldDetail, sapphireDetail, rubyDetail, diamondDetail);
    addPoint(three, 3, emeraldDetail, sapphireDetail, rubyDetail, diamondDetail);

    lineChart.addResultItem(emeraldDetail);
    lineChart.addResultItem(sapphireDetail);
    lineChart.addResultItem(rubyDetail);
    lineChart.addResultItem(diamondDetail);
    lineChart.setStart(pre2month.getMonthValue());
    return lineChart;
  }

  public List<DeltaPointInfo> getDeltaPointDetail(String userId, DeltaPointType deltaPointType) {
    LocalDate today = LocalDate.now();
    // 本周
    LocalDate thisweek = today.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));

    // 本月
    LocalDate thismonth = today.withDayOfMonth(1);

    // 三個月
    LocalDate pre2month = thismonth.minusMonths(2);

    if (deltaPointType.equals(DeltaPointType.THISWEEK)) {
      return getDeltaPointByTimePeriod(userId, toTimestamp(thisweek), toTimestamp(today), true);
    } else if (deltaPointType.equals(DeltaPointType.THISMONTH)) {
      return getDeltaPointByTimePeriod(userId, toTimestamp(thismonth), toTimestamp(today), true);
    } else {
      return getDeltaPointByTimePeriod(userId, toTimestamp(pre2month), toTimestamp(today), true);
    }
  }

  private void addPoint(
      List<DeltaPointInfo> list,
      int x,
      DeltaPointLineChartDetail emeraldDetail,
      DeltaPointLineChartDetail sapphireDetail,
      DeltaPointLineChartDetail rubyDetail,
      DeltaPointLineChartDetail diamondDetail) {
    if (null == list || list.isEmpty()) {
      list = genDefaultList();
    }

    for (DeltaPointInfo dp : list) {
      LineChartPoint lcp = new LineChartPoint();
      lcp.setX(x);
      if (dp.getLevelName().equals(EMERALD)) {
        lcp.setY(dp.getLevelCount().intValue());
        emeraldDetail.addPointsItem(lcp);
      } else if (dp.getLevelName().equals(SAPPHIRE)) {
        lcp.setY(dp.getLevelCount().intValue());
        sapphireDetail.addPointsItem(lcp);
      } else if (dp.getLevelName().equals(RUBY)) {
        lcp.setY(dp.getLevelCount().intValue());
        rubyDetail.addPointsItem(lcp);
      } else if (dp.getLevelName().equals(DIAMOND)) {
        lcp.setY(dp.getLevelCount().intValue());
        diamondDetail.addPointsItem(lcp);
      }
    }
  }

  private List<DeltaPointInfo> genDefaultList() {
    List<DeltaPointInfo> list = new ArrayList<>();
    DeltaPointInfo emePoint = new DeltaPointInfo();
    emePoint.setLevelName(EMERALD);
    emePoint.setLevelCount(0.0f);
    list.add(emePoint);
    DeltaPointInfo sapPoint = new DeltaPointInfo();
    sapPoint.setLevelName(SAPPHIRE);
    sapPoint.setLevelCount(0.0f);
    list.add(sapPoint);
    DeltaPointInfo rubyPoint = new DeltaPointInfo();
    rubyPoint.setLevelName(RUBY);
    rubyPoint.setLevelCount(0.0f);
    list.add(rubyPoint);
    DeltaPointInfo diaPoint = new DeltaPointInfo();
    diaPoint.setLevelName(DIAMOND);
    diaPoint.setLevelCount(0.0f);
    list.add(diaPoint);
    return list;
  }

  private long toTimestamp(LocalDate localDate) {
    Date date = Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    return date.getTime();
  }
}
