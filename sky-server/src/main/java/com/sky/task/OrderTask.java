package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定时任务类，定时处理订单状态
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    @Value("${sky.order.payment-timeout-minutes:30}")
    private int paymentTimeoutMinutes;

    @Value("${sky.order.delivery-timeout-hours:1}")
    private int deliveryTimeoutHours;

    /**
     * 处理超时订单——待付款超过配置分钟数则自动取消
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder() {
        log.info("定时处理超时订单，超时阈值：{} 分钟", paymentTimeoutMinutes);
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(paymentTimeoutMinutes);

        int count = orderMapper.batchCancelTimeoutOrders(
                Orders.CANCELLED,
                "超时未支付，系统自动取消",
                LocalDateTime.now(),
                Orders.PENDING_PAYMENT,
                deadline
        );

        if (count > 0) {
            log.info("已取消 {} 笔超时未支付订单", count);
        }
    }

    /**
     * 处理一直处于派送中的超时订单——超过配置小时数则自动完成
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder() {
        log.info("定时处理派送中超时订单，超时阈值：{} 小时", deliveryTimeoutHours);
        LocalDateTime deadline = LocalDateTime.now().minusHours(deliveryTimeoutHours);

        int count = orderMapper.batchCompleteDeliveryOrders(
                Orders.COMPLETED,
                LocalDateTime.now(),
                Orders.DELIVERY_IN_PROGRESS,
                deadline
        );

        if (count > 0) {
            log.info("已完成 {} 笔派送中超时订单", count);
        }
    }
}
