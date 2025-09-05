package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //1.当前集合由于存放begin到end之间所有的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            //日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //2.查询dateList集合中的日期对应的营业额数据，封装到turnoverList集合中
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //获取指定日期当天
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            //select sum(amount) from orders where order_time >= beginTime and order_time < endTime and status =5
            //将查询数据封装到Map集合中
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        //lang3这个包下，StringUtils类，使用join（连接的意思）方法将集合中的元素用逗号拼接成 字符串
        String join = StringUtils.join(dateList, ",");
        String join1 = StringUtils.join(turnoverList, ",");
        //封装返回结果
        return TurnoverReportVO
                .builder()
                .dateList(join)
                .turnoverList(join1)
                .build();
    }

    /**
     * 统计指定时间区间内的用户数据（总量和新增量）
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //1.存放从begin到end之间的每天对应的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            //日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //2.统计每天新增的用户数量
        List<Integer> newUserList = new ArrayList<>();
        //3.存放每天的总用户数量
        List<Integer> totalUserList = new ArrayList<>();

        //统计数据，查表SQL
        for (LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("end", endTime);
            //先总用户数量
            Integer totalCount =userMapper.countByMap(map);
            //再新增用户数量
            map.put("begin", beginTime);
            Integer newUserCount = userMapper.countByMap(map);
            //数据处理，如果为null，则设置为0
            totalCount = totalCount == null ? 0 : totalCount;
            newUserCount = newUserCount == null ? 0 : newUserCount;
            totalUserList.add(totalCount);
            newUserList.add(newUserCount);
        }
        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

}
