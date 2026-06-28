package com.share.order.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 订单统计查询参数
 */
@Data
public class OrderSqlVo {

    /** 统计起始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    /** 统计结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    /** @deprecated SQL 注入风险，请使用 startDate/endDate */
    @Deprecated
    private String sql;
}

