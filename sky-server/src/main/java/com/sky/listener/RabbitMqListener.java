package com.sky.listener;

import com.sky.dto.MultiDelayMessageDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RabbitMqListener {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private OrderMapper orderMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "order.delay.queue",durable = "true"),
            exchange = @Exchange(value = "order.delay.direct",delayed = "true"),
            key="delay"
    ))
    public void orderDelay(MultiDelayMessageDTO multiDelayMessageDTO) {
        Long orderId = multiDelayMessageDTO.getOrderId();
        Orders order = orderMapper.getById(orderId);
        List<Integer> delay = multiDelayMessageDTO.getDelay();
        log.info("消费者监听到延时信息:{}",multiDelayMessageDTO);

        //如果还有时间，就取出下一次延迟的时间
        if(delay!=null&&delay.size()>0){
            Integer time=delay.get(0);
            delay.remove(0);

            //未支付的订单，投送回延时消息的交换机
            if(order!=null&&order.getStatus()==Orders.PENDING_PAYMENT){
                rabbitTemplate.convertAndSend("order.delay.direct","delay",multiDelayMessageDTO,msg->{
                    msg.getMessageProperties().setDelay(time);
                    return msg;
                });
            }
        }
        else{
            //时间到就取消订单
            order.setStatus(Orders.CANCELLED);
            order.setCancelTime(LocalDateTime.now());
            order.setCancelReason("订单超时，自动取消");
        }

    }


}
