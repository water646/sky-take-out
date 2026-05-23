package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderStatisticsVO;
import org.apache.ibatis.annotations.*;

import java.util.HashMap;
import java.util.List;

@Mapper
public interface OrderMapper {

    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    Page<Orders> pageQuery(Orders order);

    @Select("select * from orders where id=#{orderId}")
    Orders getById(Long orderId);

    Page<Orders> adminPageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("select status,count(*) as counts from orders group by status")
    List<HashMap<String,Integer>> statistics();
}
