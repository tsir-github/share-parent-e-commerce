package com.share.merchant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 商家登录用户对象 share-merchant.merchant_user
 *
 * @author share
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_user")
@Schema(description = "商家登录用户")
public class MerchantUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @Schema(description = "主键ID")
    private Long id;

    /** 关联商家ID */
    @Schema(description = "关联商家ID")
    private Long merchantId;

    /** 登录账号 */
    @Schema(description = "登录账号")
    private String username;

    /** 密码（BCrypt加密） */
    @Schema(description = "密码")
    private String password;

    /** 手机号 */
    @Schema(description = "手机号")
    private String phone;

    /** 邮箱 */
    @Schema(description = "邮箱")
    private String email;

    /** 状态 0正常 1停用 */
    @Schema(description = "状态 0正常 1停用")
    private String status;

    /** 最后登录IP */
    @Schema(description = "最后登录IP")
    private String loginIp;

    /** 最后登录时间 */
    @Schema(description = "最后登录时间")
    private Date loginDate;
}
