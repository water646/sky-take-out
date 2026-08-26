package com.sky.task;

import com.sky.entity.Orders;
import com.sky.exception.OrderStatusException;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

@Slf4j
@Component
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    //处理超时订单
//    @Scheduled(cron="0 * * * * *")
//    public void processTimeoutOrder(){
//        log.info("定时处理超时订单:{}", LocalDateTime.now());
//
//        LocalDateTime latestOrderTime = LocalDateTime.now().plusMinutes(-15);
//
//        List<Orders> timeoutOrders = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, latestOrderTime);
//
//        if(timeoutOrders!=null && timeoutOrders.size()>0){
//            for(Orders order:timeoutOrders){
//                order.setStatus(Orders.CANCELLED);
//                order.setCancelTime(LocalDateTime.now());
//                order.setCancelReason("订单超时，自动取消");
//
//                //只有待支付的订单可以被取消，防止用户支付的同时订单取消了
//                int rows = orderMapper.updateCheckUnpaid(order);
//                if(rows==0){
//                    throw new OrderStatusException("订单已取消或已支付");
//                }
//            }
//        }
//    }

    //处理一直在派送中的订单
    @Scheduled(cron="0 0 1 * * *")
    public void processDeliveryOrder(){

        log.info("定时处理派送中的订单");

        LocalDateTime latestOrderTime = LocalDateTime.now().plusHours(-1);

        List<Orders> deliveryOrders = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now());

        if(deliveryOrders!=null && deliveryOrders.size()>0){
            for(Orders order:deliveryOrders){
                order.setStatus(Orders.COMPLETED);

                orderMapper.update(order);
            }
        }
    }

}
