package com.share.goods.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 用户商品收藏对象 share-goods.user_favorite
 *
 * @author share
 */
@Data
@TableName("user_favorite")
@Schema(description = "用户商品收藏")
public class UserFavorite {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "收藏时间")
    private Date createTime;
}
