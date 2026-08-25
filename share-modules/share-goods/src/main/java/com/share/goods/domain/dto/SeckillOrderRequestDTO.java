package com.share.goods.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 参与秒杀请求 DTO
 *
 * @author share
 */
@Data
@Schema(description = "参与秒杀请求")
public class SeckillOrderRequestDTO {

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量不能小于1")
    @Schema(description = "购买数量")
    private Integer quantity;

    @NotBlank(message = "收货人姓名不能为空")
    @Schema(description = "收货人姓名")
    private String receiverName;

    @NotBlank(message = "收货人电话不能为空")
    @Schema(description = "收货人电话")
    private String receiverPhone;

    @NotBlank(message = "收货地址不能为空")
    @Schema(description = "收货地址")
    private String receiverAddress;

    @Schema(description = "买家备注")
    private String remark;
}
