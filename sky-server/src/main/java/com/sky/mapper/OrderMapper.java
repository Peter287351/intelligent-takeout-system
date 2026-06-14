package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     * @param orders
     */
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

    /**
     * 修改订单状态
     * @param orderStatus
     * @param orderPaidStatus
     * @param check_out_time
     * @param id
     */
    @Update("update orders set status = #{orderStatus},pay_status = #{orderPaidStatus} ,checkout_time = #{check_out_time} where id = #{id}")
    void updateStatus(Integer orderStatus, Integer orderPaidStatus, LocalDateTime check_out_time, Long id);



    /**
     * 分页条件查询并按下单时间排序
     * @param ordersPageQueryDTO
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     * @param id
     */
    @Select("select * from orders where id=#{id}")
    Orders getById(Long id);

    /**
     * 根据状态统计订单数量
     * @param status
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * 根据动态条件统计营业额数据
     * @param map
     * @return
     */
    Double sumByMap(Map map);

    /**
     * 根据动态条件统计数据数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    /**
     * 统计指定时间区间内的销量排名top10
     * @param begin
     * @param end
     * @return
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end);

    /**
     * 批量更新订单状态——取消超时未支付订单
     * @param targetStatus 目标状态
     * @param cancelReason 取消原因
     * @param cancelTime 取消时间
     * @param currentStatus 当前状态
     * @param deadline 截止时间（下单时间早于此时间的订单）
     * @return 影响行数
     */
    @Update("update orders set status = #{targetStatus}, cancel_reason = #{cancelReason}," +
            " cancel_time = #{cancelTime} where status = #{currentStatus} and order_time < #{deadline}")
    int batchCancelTimeoutOrders(Integer targetStatus, String cancelReason, LocalDateTime cancelTime,
                                 Integer currentStatus, LocalDateTime deadline);

    /**
     * 批量更新派送中订单为已完成
     * @param targetStatus 目标状态
     * @param deliveryTime 送达时间
     * @param currentStatus 当前状态
     * @param deadline 截止时间
     * @return 影响行数
     */
    @Update("update orders set status = #{targetStatus}, delivery_time = #{deliveryTime}" +
            " where status = #{currentStatus} and order_time < #{deadline}")
    int batchCompleteDeliveryOrders(Integer targetStatus, LocalDateTime deliveryTime,
                                    Integer currentStatus, LocalDateTime deadline);
}
