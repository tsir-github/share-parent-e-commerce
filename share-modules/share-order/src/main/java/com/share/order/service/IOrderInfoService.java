package com.share.order.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.share.order.domain.OrderInfo;
import com.share.order.domain.dto.CreateOrderDTO;
import com.share.order.domain.vo.EndOrderVo;
import java.util.List;
import java.util.Map;
public interface IOrderInfoService extends IService<OrderInfo> {
    OrderInfo getByOrderNo(String orderNo);
    void processPaySuccess(String orderNo, String transactionId);
    void processRefundSuccess(String orderNo, String transactionId, java.math.BigDecimal refundAmount);
    void cancelOrder(String orderNo, String closeType, String reason);
    void deliverOrder(String orderNo, Long deliveryBy, String deliveryName, String deliveryPhone);
    void confirmReceive(String orderNo);
    String createOrder(CreateOrderDTO dto);
    void endOrder(EndOrderVo endOrderVo);
    List<OrderInfo> selectOrderListByUserId(Long userId);
    OrderInfo selectNoFinishOrder(Long userId);
    OrderInfo selectOrderInfoById(Long id);
    Map<String, Object> getOrderCount(String sql);

    /**
     * 分页查询订单列表（管理员端）
     *
     * @param orderInfo 查询条件（订单号、状态、支付状态等）
     * @return 订单列表
     */
    List<OrderInfo> selectOrderListForAdmin(OrderInfo orderInfo);
    /**
     * 统计订单仪表盘数据
     *
     * @return 包含 totalOrders, todayOrders, pendingOrders, totalRevenue, todayRevenue 的 Map
     */
    Map<String, Object> getDashboardStats();

    /**
     * 统计商家今日待发货订单数
     *
     * @param merchantId 商家ID
     * @return 今日待发货订单数
     */
    long countTodayOrders(Long merchantId);

    /**
     * 统计商家今日待发货订单销售额
     *
     * @param merchantId 商家ID
     * @return 今日待发货订单总金额
     */
    java.math.BigDecimal sumTodaySales(Long merchantId);

    /**
     * 统计商家待发货订单总数
     *
     * @param merchantId 商家ID
     * @return 待发货订单总数
     */
    long countPendingDelivery(Long merchantId);
}
