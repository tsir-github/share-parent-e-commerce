package com.share.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.coupon.domain.CouponUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户领取记录Mapper接口
 *
 * @author share
 */
public interface CouponUserMapper extends BaseMapper<CouponUser> {

    /**
     * 查询我的优惠券列表（JOIN 模板信息）
     */
    List<Map<String, Object>> selectMyCouponList(@Param("userId") Long userId,
                                                  @Param("status") String status);

    /**
     * 查询下单可用优惠券
     */
    List<Map<String, Object>> selectUsableCouponList(@Param("userId") Long userId);

    /**
     * 查询当前用户在某模板上的领取数量
     */
    Integer countClaimedByUserAndTemplate(@Param("userId") Long userId,
                                          @Param("templateId") Long templateId);

    /**
     * 批量查询当前用户在多个模板上的领取数量（替代 N+1 循环）
     *
     * @return List<Map> with keys: templateId, cnt
     */
    java.util.List<java.util.Map<String, Object>> countClaimedByUserAndTemplates(@Param("userId") Long userId,
                                                                  @Param("templateIds") java.util.List<Long> templateIds);
}
