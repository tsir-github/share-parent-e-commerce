package com.share.order.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建订单入参 DTO
 *
 * @author share
 */
@Data
@Schema(description = "创建订单请求")
public class CreateOrderDTO {

    /** 订单商品明细 */
    @NotEmpty(message = "订单商品不能为空")
    @Schema(description = "订单商品列表")
    private List<OrderItemDTO> items;

    /** 优惠券ID（可选） */
    @Schema(description = "优惠券ID")
    private Long couponId;

    /** 收货人姓名 */
    @NotBlank(message = "收货人姓名不能为空")
    @Schema(description = "收货人姓名")
    private String receiverName;

    /** 收货人电话 */
    @NotBlank(message = "收货人电话不能为空")
    @Schema(description = "收货人电话")
    private String receiverPhone;

    /** 收货地址 */
    @NotBlank(message = "收货地址不能为空")
    @Schema(description = "收货地址")
    private String receiverAddress;

    /** 买家备注 */
    @Schema(description = "买家备注")
    private String remark;

    @Data
    @Schema(description = "订单商品项")
    public static class OrderItemDTO {

        /** SKU ID */
        @NotNull(message = "商品SKU不能为空")
        @Schema(description = "商品SKU ID")
        private Long skuId;

        /** 购买数量 */
        @NotNull(message = "购买数量不能为空")
        @Schema(description = "购买数量")
        private Integer quantity;

        /** 商品名称（前端传递，服务端仅做展示存储） */
        @Schema(description = "商品名称")
        private String productName;

        /** SKU规格值（前端传递，如\"红色/XL\"） */
        @Schema(description = "SKU规格值")
        private String skuSpecs;

        /** 商品图片URL（前端传递） */
        @Schema(description = "商品图片URL")
        private String productImage;
    }
}
