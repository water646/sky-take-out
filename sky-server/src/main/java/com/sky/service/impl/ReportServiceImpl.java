package com.sky.service.impl;


import com.ctc.wstx.util.StringUtil;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
//@RequiredArgsConstructor    //这个注解只对final变量做构造器
public class ReportServiceImpl implements ReportService {

//    @Autowired
    //用构造器也能实现对象注入，让类能被完整地创建，而不是创建出来再注入属性
    private OrderMapper orderMapper;
    private UserMapper userMapper;
    private WorkspaceService workspaceService;

    public ReportServiceImpl(OrderMapper orderMapper,UserMapper userMapper,WorkspaceService workspaceService) {
        this.orderMapper=orderMapper;
        this.userMapper=userMapper;
        this.workspaceService=workspaceService;
    }

    //营业额统计
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end){

        //当前集合用于存放begin到end的日期集合
        List<LocalDate> dateList = new ArrayList();
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //遍历日期，获取每天的营业额,营业额是每天“已完成”的订单金额合计
        List<Double> turnoverList = new ArrayList<>();
        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            map.put("status", Orders.COMPLETED);

            Double amount = orderMapper.sumByMap(map);
            turnoverList.add(amount==null?0:amount);
        }

        //组装VO，把列表序列化
        TurnoverReportVO turnoverReportVO = new TurnoverReportVO();
        turnoverReportVO.setDateList(StringUtils.join(dateList, ","));
        turnoverReportVO.setTurnoverList(StringUtils.join(turnoverList, ","));

        System.out.println(turnoverReportVO);

        return turnoverReportVO;
    }

    //用户注册统计
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放begin到end的日期集合
        List<LocalDate> dateList = new ArrayList();
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        //统计每天新注册的用户数量
        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("endTime", endTime);

            //每日新增用户数量
            Integer total = userMapper.countByDate(map);
            totalUserList.add(total);

            //总用户数量
            map.put("beginTime", beginTime);
            Integer sum = userMapper.countByDate(map);
            newUserList.add(sum);
        }

//        System.out.println(totalUserList);

        UserReportVO userReportVO = new UserReportVO();
        userReportVO.setDateList(StringUtils.join(dateList, ","));
        userReportVO.setTotalUserList(StringUtils.join(totalUserList, ","));
        userReportVO.setNewUserList(StringUtils.join(newUserList, ","));

        return userReportVO;
    }

    //订单统计
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end){

        List<LocalDate> dateList = new ArrayList();
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        Integer totalOrderCount = 0;
        Integer totalValidOrderCount = 0;
        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            //查询订单和有效订单数量
            Map map = new HashMap();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);

            Integer oneDayCount = orderMapper.countByMap(map);
            totalOrderCount += oneDayCount;
            orderCountList.add(oneDayCount);

            map.put("status", Orders.COMPLETED);
            Integer oneDayValidCount = orderMapper.countByMap(map);
            totalValidOrderCount += oneDayValidCount;
            validOrderCountList.add(oneDayValidCount);
        }

        //计算订单完成率
        Double orderCompletionRate =0.0;
        if(totalOrderCount!=0){
            orderCompletionRate = totalValidOrderCount.doubleValue() / totalOrderCount;
        }

        //拼装VO
        OrderReportVO orderReportVO = new OrderReportVO();
        orderReportVO.setDateList(StringUtils.join(dateList, ","));
        orderReportVO.setOrderCountList(StringUtils.join(orderCountList, ","));
        orderReportVO.setValidOrderCountList(StringUtils.join(validOrderCountList, ","));
        orderReportVO.setTotalOrderCount(totalOrderCount);
        orderReportVO.setValidOrderCount(totalValidOrderCount);
        orderReportVO.setOrderCompletionRate(orderCompletionRate);

//        System.out.println(orderReportVO);

        return orderReportVO;
    }

    //销量统计
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {


        List<GoodsSalesDTO> goodsList = new ArrayList<>();

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        Map map = new HashMap();
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        map.put("status", Orders.COMPLETED);

        goodsList = orderMapper.getSalesTop(map);

        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        for(GoodsSalesDTO goodsSalesDTO : goodsList){
            nameList.add(goodsSalesDTO.getName());
            numberList.add(goodsSalesDTO.getNumber());
        }

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList,","))
                .numberList(StringUtils.join(numberList,","))
                .build();
    }

    //导出数据报表
    public void exportBusinessData(HttpServletResponse response) throws IOException {

        //查询概览数据
        LocalDate beginDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now().minusDays(1);

        LocalDateTime beginTime = LocalDateTime.of(beginDate, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(endDate, LocalTime.MAX);


        System.out.println(beginTime);
        System.out.println(endTime);
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(beginTime, endTime);

        //把数据放进excel表中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        //基于模板文件，创建新的excel
        try{
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //填充excel数据
            XSSFSheet sheet = excel.getSheet("Sheet1");
            XSSFRow row4 = sheet.getRow(3);
            XSSFRow row5 = sheet.getRow(4);

            sheet.getRow(1).getCell(1).setCellValue("时间:"+beginDate+"至"+endDate);
            row4.getCell(2).setCellValue(businessDataVO.getTurnover());
            row4.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row4.getCell(6).setCellValue(businessDataVO.getNewUsers());
            row5.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row5.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            for(int i=0;i<30;i++){
                LocalDate date = beginDate.plusDays(i);
                //查询某一天的营业数据
                businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                XSSFRow currentRow = sheet.getRow(7+i);
                currentRow.getCell(1).setCellValue(date.toString());
                currentRow.getCell(2).setCellValue(businessDataVO.getTurnover());
                currentRow.getCell(3).setCellValue(businessDataVO.getValidOrderCount());
                currentRow.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
                currentRow.getCell(5).setCellValue(businessDataVO.getUnitPrice());
                currentRow.getCell(6).setCellValue(businessDataVO.getNewUsers());
            }


            //通过输出流下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();

        }catch (Exception e){
            e.printStackTrace();
        }





    }
}
