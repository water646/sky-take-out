package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {

    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderVO getOrderDetail(Long orderId);

    void cancelOrder(OrdersCancelDTO ordersCancelDTO);

    void repeatOrder(Long orderId);

    PageResult adminPageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO orderStatistics();

    void confirmOrder(Long orderId);

    void rejectOrder(OrdersRejectionDTO ordersRejectionDTO);

    void orderDelivery(Long orderId);

    void orderComplete(Long orderId);
}
