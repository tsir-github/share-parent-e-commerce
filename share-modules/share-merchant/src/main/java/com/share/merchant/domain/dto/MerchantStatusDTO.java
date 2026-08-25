package com.share.merchant.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商家状态变更请求
 *
 * @author share
 */
@Data
@Schema(description = "商家状态变更请求")
public class MerchantStatusDTO {

    @NotNull(message = "商家ID不能为空")
    @Schema(description = "商家ID")
    private Long id;

    @NotBlank(message = "状态不能为空")
    @Schema(description = "状态：0待审核 1已启用 2已关闭")
    private String status;
}
