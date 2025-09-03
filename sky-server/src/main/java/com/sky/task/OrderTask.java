package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类，定时处理订单状态
 */
@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    /**
     * 处理超时订单的方法，超过15分钟未支付则取消订单
     */
    //每分钟执行一次
    @Scheduled(cron = "0 * * * * ?")
    //@Scheduled(cron = "2/5 * * * * ?")
    public void processTimeoutOrder(){
        log.info("定时处理超时订单:{}", LocalDateTime.now());

        //具体业务逻辑相关的代码
        //获取所有15分钟前的订单（集合），并且订单状态为待付款
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);

        //遍历集合，处理订单状态
        if (ordersList != null && ordersList.size() > 0){
            for (Orders orders : ordersList) {
                //更新状态，取消超时订单
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 处理一直处于派送中的超时订单
     */
    //每日1点执行
    @Scheduled(cron = "0 0 1 * * ?")
    //@Scheduled(cron = "0/5 * * * * ?")
    public void processDeliveryOrder(){
        log.info("定时处理派送中订单:{}", LocalDateTime.now());
        //获取所有派送中的订单（集合），并且订单状态为派送中
        LocalDateTime time = LocalDateTime.now().plusHours(-1);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        if (ordersList != null && ordersList.size() > 0){
            for (Orders orders : ordersList) {
                //更新状态，派送中订单改为已完成
                orders.setStatus(Orders.COMPLETED);
                orders.setDeliveryTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }

    }
}
