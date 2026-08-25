package com.share.merchant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 商家信息对象 share-merchant.merchant_info
 *
 * @author share
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_info")
@Schema(description = "商家信息")
public class MerchantInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    ///** 商家ID */
    //@Schema(description = "ID")
    //private Long id;

    /** 关联商家用户ID */
    @Schema(description = "关联商家用户ID")
    private Long userId;

    /** 店铺名称 */
    @Schema(description = "店铺名称")
    private String name;

    /** 联系人 */
    @Schema(description = "联系人")
    private String contactName;

    /** 联系电话 */
    @Schema(description = "联系电话")
    private String contactPhone;

    /** 店铺地址 */
    @Schema(description = "店铺地址")
    private String address;

    /** 店铺logo */
    @Schema(description = "店铺logo")
    private String logo;

    /** 店铺描述 */
    @Schema(description = "店铺描述")
    private String description;

    /** 状态：0待审核 1已启用 2已关闭 */
    @Schema(description = "状态：0待审核 1已启用 2已关闭")
    private String status;

    /** 审核备注 */
    @Schema(description = "审核备注")
    private String auditRemark;

    /** 审核时间 */
    @Schema(description = "审核时间")
    private Date auditTime;
}
