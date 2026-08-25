package com.share.merchant.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 重置商家密码请求
 *
 * @author share
 */
@Data
@Schema(description = "重置商家密码请求")
public class ResetPasswordDTO {

    @NotNull(message = "商家ID不能为空")
    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "新密码（为空时使用默认密码 123456）")
    private String newPassword;
}
