package com.share.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.user.domain.UserInfo;
import com.share.user.domain.dto.WxLoginResultDTO;
import com.share.user.domain.vo.UserVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 用户 Service 接口
 *
 * @author share
 */
public interface IUserInfoService extends IService<UserInfo> {

    /**
     * 查询用户列表
     */
    List<UserInfo> selectUserInfoList(UserInfo userInfo);

    /**
     * 微信授权登录
     */
    WxLoginResultDTO wxLogin(String code);

    /**
     * 获取当前登录用户 VO
     */
    UserVo getLoginUserVo(Long userId);

    /**
     * 统计用户注册数据
     */
    Map<String, Object> getUserCount();

    /**
     * 统计用户仪表盘数据
     *
     * @return 包含 totalUsers, todayNewUsers 的 Map
     */
    Map<String, Object> getDashboardStats();

    /**
     * 更新用户头像
     *
     * @param file 头像图片文件
     * @return 新头像URL
     */
    String updateAvatar(MultipartFile file);

    /**
     * 更新用户昵称
     *
     * @param nickname 新昵称（1-30字符）
     */
    void updateNickname(String nickname);

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    UserInfo getCurrentUser();

    /**
     * 用户统计数据（平台报表用）
     *
     * @return totalUsers, todayNewUsers, weekNewUsers, monthNewUsers, activeUsers, dailyTrend
     */
    Map<String, Object> getUserStatistics();
}
