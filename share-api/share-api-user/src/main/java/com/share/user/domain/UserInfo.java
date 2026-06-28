package com.share.user.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.share.common.core.annotation.Excel;
import com.share.common.core.web.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 用户对象 user_info
 *
 * @author atguigu
 * @date 2025-02-17
 */
@Data
@TableName("user_info")
@Schema(description = "用户")
public class UserInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 微信openId */
    @Excel(name = "微信openId")
    @Schema(description = "微信openId")
    private String wxOpenId;

    /** 会员昵称 */
    @Excel(name = "会员昵称")
    @Schema(description = "会员昵称")
    private String nickname;

    /** 性别 */
    @Excel(name = "性别")
    @Schema(description = "性别")
    private String gender;

    /** 头像 */
    @Excel(name = "头像")
    @Schema(description = "头像")
    private String avatarUrl;

    /** 电话 */
    @Excel(name = "电话")
    @Schema(description = "电话")
    private String phone;

    /** 最后一次登录ip */
    @Excel(name = "最后一次登录ip")
    @Schema(description = "最后一次登录ip")
    private String lastLoginIp;

    /** 最后一次登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后一次登录时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后一次登录时间")
    private Date lastLoginTime;

    /** 0正常 1停用 */
    @Excel(name = "0正常 1停用")
    @Schema(description = "0正常 1停用")
    private String status;

}
