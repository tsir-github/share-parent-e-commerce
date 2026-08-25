package com.share.merchant.domain.vo;

import com.share.merchant.domain.MerchantInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商家列表 VO — 附带关联登录账号信息
 *
 * @author share
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantInfoVO extends MerchantInfo {

    /** 关联登录账号用户名 */
    private String accountUsername;

    /** 关联登录账号状态：0正常 1停用 */
    private String accountStatus;
}
