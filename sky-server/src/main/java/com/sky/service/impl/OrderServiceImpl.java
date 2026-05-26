package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WebSocketServer webSocketServer;

    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        //处理各种业务异常（购物车为空、没有收货地址）
        Long userId = BaseContext.getCurrentId();

        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList.size() == 0||shoppingCartList==null) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook==null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }


        //向订单表插入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);

        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);

        orderMapper.insert(orders);

        //向订单明细表插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();

        for(ShoppingCart cart:shoppingCartList){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(orders.getId());

            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);

        //下单成功后清空用户的购物车
        shoppingCartMapper.deleteByUserId(userId);

        //封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .build();

        return orderSubmitVO;

    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.updateCheckUnpaid(orders);

        //通过websocket向商家端发送支付完成提醒
        Map map = new HashMap();
        map.put("type",1);      //1表示来单提醒，2表示催单
        map.put("orderId",orders.getId());
        map.put("content","订单号:"+outTradeNo);
        String json = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(json);

    }

    //分页查询
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {

        Long userId = BaseContext.getCurrentId();
        Orders order = Orders.builder()
                        .userId(userId)
                        .status(ordersPageQueryDTO.getStatus())
                        .build();

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> orders = orderMapper.pageQuery(order);

        //订单实体列表向订单VO列表转换，再填入详情列表
        List<OrderVO> orderVOs = new ArrayList<>();
        List<Orders> orderList = orders.getResult();
        for(Orders o: orderList){
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(o, orderVO);
            orderVOs.add(orderVO);

            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(o.getId());
            orderVO.setOrderDetailList(orderDetailList);
        }


        PageResult pageResult = new PageResult();
        pageResult.setTotal(orders.getTotal());
        pageResult.setRecords(orderVOs);

        return pageResult;
    }

    //查看订单详情
    public OrderVO getOrderDetail(Long orderId) {

        Orders orders = orderMapper.getById(orderId);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);

        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    //取消订单
    public void cancelOrder(OrdersCancelDTO ordersCancelDTO) {
        Orders order = orderMapper.getById(ordersCancelDTO.getId());
        String cancelReason = ordersCancelDTO.getCancelReason();

        if(order.getStatus() > Orders.TO_BE_CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //如果用户已支付，则需要退款
        if(order.getPayStatus() == Orders.PAID){
            order.setPayStatus(Orders.REFUND);
        }

        order.setStatus(Orders.CANCELLED);
        order.setCancelTime(LocalDateTime.now());

        order.setCancelReason(cancelReason);

        //只有待支付状态的订单可以取消，防止支付与取消并发
        orderMapper.updateCheckUnpaid(order);
    }

    //再来一单
    public void repeatOrder(Long orderId) {
        Orders order = orderMapper.getById(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);

        for(OrderDetail orderDetail: orderDetailList){
            ShoppingCart cart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail,cart);

            cart.setUserId(order.getUserId());
            cart.setCreateTime(LocalDateTime.now());
            cart.setId(null);
            shoppingCartMapper.insert(cart);
        }

    }

    //管理端分页查询
    public PageResult adminPageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> orders = orderMapper.adminPageQuery(ordersPageQueryDTO);

        PageResult pageResult = new PageResult();
        pageResult.setTotal(orders.getTotal());
        pageResult.setRecords(orders.getResult());

        return pageResult;
    }

    //各状态订单统计数据
    public OrderStatisticsVO orderStatistics() {
        //获取订单状态-数量的一组哈希表
        List<HashMap<String,Integer>> statusCount = orderMapper.statistics();

        //把相应状态的数量填进VO
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        for(HashMap map: statusCount){
            Number longCount =(Number) map.get("counts");
            Integer count =longCount.intValue();

            if(map.get("status").equals(Orders.TO_BE_CONFIRMED)){
                orderStatisticsVO.setConfirmed(count);
            }
            else if(map.get("status").equals(Orders.CONFIRMED)){
                orderStatisticsVO.setConfirmed(count);
            }
            else if(map.get("status").equals(Orders.DELIVERY_IN_PROGRESS)){
                orderStatisticsVO.setDeliveryInProgress(count);
            }
        }


        return orderStatisticsVO;
    }

    //接单
    public void confirmOrder(Long orderId) {
        Orders order = Orders.builder()
                        .id(orderId)
                        .status(Orders.CONFIRMED)
                        .build();

        orderMapper.update(order);
    }

    //拒单
    public void rejectOrder(OrdersRejectionDTO ordersRejectionDTO) {

        //待接单状态才能拒单
        Orders originOrder = orderMapper.getById(ordersRejectionDTO.getId());
        Integer status = originOrder.getStatus();
        if(status == null || status > Orders.TO_BE_CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Integer payStatus = originOrder.getPayStatus();
        if(payStatus != null && payStatus == Orders.PAID){
            log.info("申请退款");
        }

        Orders order = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();

        orderMapper.updateCheckUnpaid(order);
    }

    //派送订单
    public void orderDelivery(Long id){
        Orders order = orderMapper.getById(id);
        order.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(order);
    }

    //订单完成
    public void orderComplete(Long id){
        Orders order = new Orders();
        order.setStatus(Orders.COMPLETED);
        order.setId(id);

        orderMapper.update(order);
    }

    //用户催单
    public void reminder(Long orderId){

        Orders order = orderMapper.getById(orderId);

        if(order==null){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Map map = new HashMap();
        map.put("orderId", orderId);
        map.put("type",2);
        map.put("content","订单号:"+order.getNumber());
        String json = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(json);
    }

}
