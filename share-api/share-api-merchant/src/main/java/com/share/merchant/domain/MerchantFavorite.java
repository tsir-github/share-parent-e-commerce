package com.share.merchant.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 商家收藏对象 merchant_favorite
 *
 * @author share
 */
@Data
@TableName("merchant_favorite")
@Schema(description = "商家收藏")
public class MerchantFavorite implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "商家ID")
    private Long merchantId;

    @Schema(description = "创建时间")
    private Date createTime;
}
