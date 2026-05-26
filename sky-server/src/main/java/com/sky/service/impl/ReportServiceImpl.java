package com.sky.service.impl;


import com.ctc.wstx.util.StringUtil;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    private OrderDetailMapper orderDetailMapper;

    public ReportServiceImpl(OrderMapper orderMapper,UserMapper userMapper) {
        this.orderMapper=orderMapper;
        this.userMapper=userMapper;
        this.orderDetailMapper=orderDetailMapper;
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
            Integer sum = userMapper.countByDate(map);
            newUserList.add(sum);

            //总用户数量
            map.put("beginTime", beginTime);
            Integer total = userMapper.countByDate(map);
            totalUserList.add(total);

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
}
