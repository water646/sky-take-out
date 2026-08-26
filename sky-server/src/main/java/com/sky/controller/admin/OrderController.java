package com.sky.controller.admin;


import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@Api(tags="订单管理相关接口")
@RequestMapping("/admin/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/conditionSearch")
    @ApiOperation("查询订单")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {

        PageResult pageResult = orderService.adminPageQuery(ordersPageQueryDTO);

        return Result.success(pageResult);
    }

    @GetMapping("/statistics")
    @ApiOperation("各个状态的订单数量统计")
    public Result<OrderStatisticsVO> statistics() {
        OrderStatisticsVO orderStatisticsVO = orderService.orderStatistics();

        return Result.success(orderStatisticsVO);
    }

    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        Long id = ordersConfirmDTO.getId();

        orderService.confirmOrder(id);

        return Result.success();
    }

    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result reject(@RequestBody OrdersRejectionDTO ordersRejectionDTO) {

        orderService.rejectOrder(ordersRejectionDTO);

        return Result.success();
    }

    @PutMapping("/cancel")
    @ApiOperation("商家取消订单")
    public Result cancel(OrdersCancelDTO ordersCancelDTO) {

        orderService.cancelOrder(ordersCancelDTO);

        return Result.success();
    }

    @PutMapping("/delivery/{id}")
    @ApiOperation("派送订单")
    public Result delivery(@PathVariable Long id) {

        orderService.orderDelivery(id);

        return Result.success();
    }

    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result complete(@PathVariable Long id) {

        orderService.orderComplete(id);

        return Result.success();
    }

    @GetMapping("/details/{id}")
    @ApiOperation("订单详情")
    public Result details(@PathVariable Long id) {
        OrderVO orderVO = orderService.getOrderDetail(id);
        return Result.success(orderVO);
    }


}
