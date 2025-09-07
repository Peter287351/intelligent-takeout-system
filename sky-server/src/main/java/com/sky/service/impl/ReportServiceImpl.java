package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;
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

    /**
     * 统计指定时间区间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        //1.存放从begin到end之间的每天对应的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //存放每天的订单总数和有效订单数
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        //2.遍历dateList集合,查询每天的有效订单数和订单总数
        for (LocalDate date : dateList){
            //查询每天的订单总数，select count(id) from orders where order_time >= beginTime and order_time < endTime
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer orderCount = getOrderCount(beginTime, endTime, null);

            //查询每天的有效订单数，select count(id) from orders where order_time >= beginTime and order_time < endTime and status =5
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            //将查询出来的数据添加到对应的集合中
            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        //3.不查询数据库，遍历时间区间内的订单数量，这里使用Stream流，将集合里面的所有元素累加到一起
        //3.1计算时间区间内的订单总数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        //3.2计算时间区间内的有效订单数量
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        //4.计算订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            //转为double
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }
        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }
    //根据条件统计订单数量
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);

        return orderMapper.countByMap(map);
    }

    /**
     * 统计指定时间区间内的销量排名top10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        //将List集合（DTO）处理为VO类型，通过Stream流进行转换
        //1.将DTO的name属性值取出来，组成一个新的List集合
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        //转化为字符串，用逗号分隔
        String nameList = StringUtils.join(names, ",");
        //2.将DTO的number属性值取出来，组成一个新的List集合
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        //转化为字符串，用逗号分隔
        String numberList = StringUtils.join(numbers, ",");

        //封装VO对象并返回
        return SalesTop10ReportVO
                .builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //1.查询数据库，获取营业数据---查询最近30天的营业数据
        //这里使用LocalDate，因为LocalDateTime不能进行日期的运算
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now();
        //转为LocalDateTime，因为数据库的营业数据是按照LocalDateTime进行保存的
        LocalDateTime begin = LocalDateTime.of(dateBegin, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(dateEnd, LocalTime.MAX);
        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(begin, end);
        //2.通过POI将营业数据写入到Excel文件中
        //获取输入流对象
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            //基于已有模板文件，创建新的Excel文件对象
            XSSFWorkbook excel = new XSSFWorkbook(in);
            //获取表格文件sheet标签页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //填充数据---时间
            //这里不用创建行，因为这个行已经存在了，直接获取（get）行即可
            sheet.getRow(1).getCell(1).setCellValue("时间"+dateBegin + "至" + dateEnd);

            //填充数据---概览数据
            //获取第四行
            XSSFRow row4 = sheet.getRow(3);
            row4.getCell(2).setCellValue(businessDataVO.getTurnover());
            row4.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row4.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //获取第五行
            XSSFRow row5 = sheet.getRow(4);
            row5.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row5.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充数据---明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                //获取某一行
                XSSFRow row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessDataVO.getTurnover());
                row.getCell(3).setCellValue(businessDataVO.getValidOrderCount());
                row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessDataVO.getUnitPrice());
                row.getCell(6).setCellValue(businessDataVO.getNewUsers());
            }

            //3.通过输出流，将Excel文件下载到客户端浏览器上
            //获取输出流对象
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            //关闭流对象
            out.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
