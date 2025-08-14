package com.share.device.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "充电宝柜机")
public class Cabinet extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 机柜编号 */
    @Schema(description = "机柜编号")
    private String cabinetNo;

    /** 名称 */
    @Schema(description = "名称")
    private String name;

    /** 类别id */
    @Schema(description = "类别id")
    private Long cabinetTypeId;

    /** 总插槽数量 */
    @Schema(description = "总插槽数量")
    private Integer totalSlots;

    /** 空闲插槽数量 */
    @Schema(description = "空闲插槽数量")
    private Integer freeSlots;

    /** 已使用插槽数量 */
    @Schema(description = "已使用插槽数量")
    private Integer usedSlots;

    /** 可用充电宝数量 */
    @Schema(description = "可用充电宝数量")
    private Integer availableNum;

    /** 描述 */
    @Schema(description = "描述")
    private String description;

    /** 当前位置id */
    @Schema(description = "当前位置id")
    private Long locationId;

    /** 状态（0：未投入 1：使用中 -1：故障） */
    @Schema(description = "状态")
    private String status;

    @Schema(description = "分类名称")
    @TableField(exist = false)//标识非数据库字段,当实体类的某个字段
    // 不需要与数据库表的列映射 时，使用此注解明确标记
    private String cabinetTypeName;

    //批量查询字段
    @TableField(exist = false)//标识非数据库字段
    private List<Long> ids;

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

}