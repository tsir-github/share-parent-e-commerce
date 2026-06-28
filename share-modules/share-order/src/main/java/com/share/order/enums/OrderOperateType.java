package com.share.order.enums;

/**
 * 订单操作类型枚举
 *
 * @author share
 */
public enum OrderOperateType
{
    CREATE("0", "下单"),
    PAY("1", "支付"),
    DELIVER("2", "发货"),
    ARRIVE("3", "配送员送达"),
    CONFIRM("4", "确认收货"),
    CANCEL("5", "取消"),
    AFTER_SALE("6", "售后申请"),
    REFUND("7", "同意退款");

    private final String code;
    private final String desc;

    OrderOperateType(String code, String desc)
    {
        this.code = code;
        this.desc = desc;
    }

    public String getCode()
    {
        return code;
    }

    public String getDesc()
    {
        return desc;
    }
}
