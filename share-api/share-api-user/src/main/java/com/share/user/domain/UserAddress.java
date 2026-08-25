package com.share.user.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户收货地址对象 user_address
 *
 * @author share
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_address")
@Schema(description = "用户收货地址")
public class UserAddress extends BaseEntity {

    private static final long serialVersionUID = 1L;

    ///** 地址ID */
    //@Schema(description = "ID")
    //private Long id;

    /** 用户ID */
    @Schema(description = "用户ID")
    private Long userId;

    /** 收货人姓名 */
    @Schema(description = "收货人姓名")
    private String name;

    /** 收货人电话 */
    @Schema(description = "收货人电话")
    private String phone;

    /** 地区编码 */
    @Schema(description = "地区编码")
    private String regionCode;

    /** 省市区 */
    @Schema(description = "省市区")
    private String region;

    /** 详细地址 */
    @Schema(description = "详细地址")
    private String detail;

    /** 是否默认地址（0否 1是） */
    @Schema(description = "是否默认地址")
    private Boolean isDefault;

    /** 扩展字段 */
    @Schema(description = "扩展字段JSON")
    private String extJson;
}
