package com.share.order.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ID 分配器对象 share_order.id_allocator
 *
 * @author share
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("id_allocator")
@Schema(description = "ID分配器")
public class IdAllocator extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 业务标识 */
    @Schema(description = "业务标识")
    private String bizTag;

    /** 当前最大ID */
    @Schema(description = "当前最大ID")
    private Long maxId;

    /** 步长 */
    @Schema(description = "步长")
    private Integer step;

    /** 版本号 */
    @Schema(description = "版本号")
    private Long version;

}
