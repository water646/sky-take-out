package com.sky.controller.user;


import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.Orders;
import com.sky.exception.OrderStatusException;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

@RestController("userOrderController")  //防止与商家端controller在spring容器中重名
@RequestMapping("/user/order")
@Slf4j
@Api(tags="用户端订单相关接口")
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {

        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);

        //为防止重复支付，根据订单号生成一个键
        redisTemplate.opsForValue().set("payment_lock:"+orderSubmitVO.getOrderNumber(),"Order Created",16, TimeUnit.MINUTES);

        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);


        //如果删键成功，则支付成功，后来的请求都会删键失败，防止重复支付
        if(redisTemplate.delete("payment_lock:"+ordersPaymentDTO.getOrderNumber())){
            //如果paySuccess失败，就把redis的key放回去，实现回滚
            try{
                orderService.paySuccess(ordersPaymentDTO.getOrderNumber());
                log.info("支付成功");
                return Result.success();
            }catch (OrderStatusException e){
                redisTemplate.opsForValue().set("payment_lock:"+ordersPaymentDTO.getOrderNumber(),"Order Created",16, TimeUnit.MINUTES);
                return Result.error(e.getMessage());
            }
        }
        else {
            log.info("支付失败请重试");
            return Result.error("支付失败，请重试");
        }

//        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
//        log.info("生成预支付交易单：{}", orderPaymentVO);

    }

    @GetMapping("/historyOrders")
    @ApiOperation("查询历史订单")
    public Result<PageResult> historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {

        PageResult pageResult = orderService.pageQuery(ordersPageQueryDTO);

        Orders order1 = (Orders)pageResult.getRecords().get(0);

        log.info("orderId是{}",order1.getId());

        return Result.success(pageResult);
    }

    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> orderDetail(@PathVariable Long id) {

        OrderVO orderVO = orderService.getOrderDetail(id);

        return Result.success(orderVO);
    }

    @PutMapping("/cancel/{id}")
    @ApiOperation("用户取消订单")
    public Result cancel(@PathVariable Long id) {
        OrdersCancelDTO  ordersCancelDTO = new OrdersCancelDTO();
        ordersCancelDTO.setId(id);
        ordersCancelDTO.setCancelReason("用户取消");
        orderService.cancelOrder(ordersCancelDTO);

        return Result.success();
    }

    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result repetition(@PathVariable Long id) {

        orderService.repeatOrder(id);

        return Result.success();
    }

    @GetMapping("/reminder/{id}")
    @ApiOperation("催单")
    public Result reminder(@PathVariable Long id) {
        orderService.reminder(id);

        return  Result.success();
    }



}
