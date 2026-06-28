package com.share.user.mapper;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.user.domain.vo.UserCountVo;
import com.share.user.domain.UserInfo;
import org.apache.ibatis.annotations.Param;

/**
 * 用户Mapper接口
 *
 * @author atguigu
 * @date 2025-02-17
 */
public interface UserInfoMapper extends BaseMapper<UserInfo>
{

    /**
     * 查询用户列表
     *
     * @param userInfo 用户
     * @return 用户集合
     */
    public List<UserInfo> selectUserInfoList(UserInfo userInfo);

    //统计2024年每个月注册人数
    //远程调用：统计用户注册数据
    List<UserCountVo> selectUserCount();

    /** 新增用户趋势（按天） */
    List<Map<String, Object>> selectNewUserTrend(@Param("startDate") String startDate);

    /** 活跃用户数（最近30天有登录） */
    Integer countActiveUsers();
}
