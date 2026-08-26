package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     *
     * @param orders
     * @return
     */
    int update(Orders orders);

    //修改订单状态前，检查订单状态为待支付，防止取消和支付并发修改状态导致混乱
    int updateCheckUnpaid(Orders order);

    Page<Orders> pageQuery(Orders order);

    @Select("select * from orders where id=#{orderId}")
    Orders getById(Long orderId);

    Page<Orders> adminPageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("select status,count(*) as counts from orders group by status")
    List<HashMap<String,Integer>> statistics();

    //查询未付款的超时订单
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTimeLT(@Param("status") Integer status, @Param("orderTime") LocalDateTime orderTime);

    Double sumByMap(Map map);

    Integer countByMap(Map map);

    //select od.name as name,SUM(od.number) as sales from order_detail od join orders o on od.order_id = o.id where o.order_time > ? and o.order_time < ? and o.status = ? group by od.name order by sales desc limit 0,10
    List<GoodsSalesDTO> getSalesTop(Map map);
}
