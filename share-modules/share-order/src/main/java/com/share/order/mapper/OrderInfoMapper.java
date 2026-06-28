package com.share.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.order.domain.OrderInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 订单Mapper接口
 *
 * @author atguigu
 * @date 2024-10-25
 */
public interface OrderInfoMapper extends BaseMapper<OrderInfo>
{

    /**
     * 乐观锁更新订单状态
     *
     * @param id           订单ID
     * @param status       目标状态
     * @param version      当前版本号（乐观锁条件）
     * @return 影响行数（0=更新失败，数据被修改）
     */
    int updateOrderStatus(@Param("id") Long id, @Param("status") String status,
                          @Param("version") Integer version, @Param("expectedStatus") String expectedStatus);

    /**
     * 按日期范围统计订单数量
     */
    List<Map<String, Object>> getOrderCountByDate(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * @deprecated SQL 注入风险，使用 getOrderCountByDate 替代
     */
    @Deprecated
    List<Map<String, Object>> getOrderCount(String sql);
}
