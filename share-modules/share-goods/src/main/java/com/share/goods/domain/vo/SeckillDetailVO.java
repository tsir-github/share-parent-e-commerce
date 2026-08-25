package com.share.goods.domain.vo;

import com.share.goods.domain.SeckillActivity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 秒杀活动详情 VO（C端）
 *
 * @author share
 */
@Data
@Schema(description = "秒杀活动详情VO")
public class SeckillDetailVO {

    @Schema(description = "活动ID")
    private Long id;

    @Schema(description = "活动名称")
    private String name;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品主图")
    private String mainImage;

    @Schema(description = "SKU规格JSON")
    private String skuSpecs;

    @Schema(description = "秒杀价")
    private BigDecimal seckillPrice;

    @Schema(description = "商品原价")
    private BigDecimal originPrice;

    @Schema(description = "剩余库存")
    private Integer stock;

    @Schema(description = "总库存")
    private Integer totalStock;

    @Schema(description = "每人限购")
    private Integer limitPerUser;

    @Schema(description = "当前用户已购数量")
    private Integer userPurchasedCount;

    @Schema(description = "是否已达限购上限")
    private Boolean limitReached;

    @Schema(description = "状态(0-未开始 1-进行中 2-已结束 3-已禁用)")
    private String status;

    @Schema(description = "活动开始时间")
    private Date startTime;

    @Schema(description = "活动结束时间")
    private Date endTime;

    @Schema(description = "已售数量")
    private Integer salesCount;

    public static SeckillDetailVO from(SeckillActivity a) {
        SeckillDetailVO vo = new SeckillDetailVO();
        vo.setId(a.getId());
        vo.setName(a.getName());
        vo.setProductId(a.getProductId());
        vo.setSkuId(a.getSkuId());
        vo.setProductName(a.getProductName());
        vo.setMainImage(a.getMainImage());
        vo.setSkuSpecs(a.getSkuSpecs());
        vo.setSeckillPrice(a.getSeckillPrice());
        vo.setOriginPrice(null);
        vo.setStock(a.getStock());
        vo.setTotalStock(a.getStock());
        vo.setLimitPerUser(a.getLimitPerUser());
        vo.setUserPurchasedCount(0);
        vo.setLimitReached(false);
        vo.setStatus(a.getStatus());
        vo.setStartTime(a.getStartTime());
        vo.setEndTime(a.getEndTime());
        vo.setSalesCount(0);
        return vo;
    }
}
