package com.share.user.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.user.domain.vo.UserCountVo;
import com.share.user.domain.vo.UserVo;
import com.share.user.domain.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.utils.StringUtils;
import com.share.common.security.service.TokenService;
import com.share.common.security.utils.SecurityUtils;
import com.share.system.api.RemoteFileService;
import com.share.system.api.domain.SysFile;
import com.share.system.api.model.LoginUser;
import com.share.user.domain.dto.WxLoginResultDTO;
import com.share.user.mapper.UserInfoMapper;
import com.share.user.service.IUserInfoService;
import org.springframework.web.multipart.MultipartFile;
/**
 * 用户Service业务层处理
 *
 * @author atguigu
 * @date 2025-02-17
 */
@Service
@RequiredArgsConstructor
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService
{
    private final UserInfoMapper userInfoMapper;
    private final WxMaService wxMaService;
    private final RemoteFileService remoteFileService;
    private final TokenService tokenService;

    /**
     * 查询用户列表
     *
     * @param userInfo 用户
     * @return 用户
     */
    public List<UserInfo> selectUserInfoList(UserInfo userInfo)
    {
        return userInfoMapper.selectUserInfoList(userInfo);
    }

    //微信授权登录-远程调用
    @Override
    public WxLoginResultDTO wxLogin(String code) {
        try {
            WxMaJscode2SessionResult sessionInfo =
                    wxMaService.getUserService().getSessionInfo(code);
            String openid = sessionInfo.getOpenid();

            LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserInfo::getWxOpenId, openid);
            UserInfo userInfo = userInfoMapper.selectOne(wrapper);

            boolean isNewUser = false;
            if (userInfo == null) {
                userInfo = new UserInfo();
                userInfo.setNickname(String.valueOf(System.currentTimeMillis()));
                // ponytail: avatar empty by default, frontend shows fallback icon
                userInfo.setAvatarUrl("");
                userInfo.setWxOpenId(openid);
                userInfoMapper.insert(userInfo);
                isNewUser = true;
            }

            // 构建 LoginUser 并生成 token
            LoginUser loginUser = new LoginUser();
            loginUser.setUserid(userInfo.getId());
            loginUser.setUsername(StringUtils.isNotEmpty(userInfo.getNickname())
                    ? userInfo.getNickname() : openid);
            Map<String, Object> tokenMap = tokenService.createToken(loginUser);
            String token = (String) tokenMap.get("access_token");

            return new WxLoginResultDTO(token, userInfo, isNewUser);
        } catch (Exception e) {
            throw new ServiceException("微信登录失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户 VO
     */
    public UserVo getLoginUserVo(Long userId) {
        UserInfo userInfo = this.getById(userId);
        if (userInfo == null) {
            return null;
        }
        UserVo vo = new UserVo();
        vo.setNickname(userInfo.getNickname());
        vo.setAvatar(userInfo.getAvatarUrl());
        vo.setWxOpenId(userInfo.getWxOpenId());
        return vo;
    }

    //统计2024年每个月注册人数
    //远程调用：统计用户注册数据
    public Map<String, Object> getUserCount() {
        List<UserCountVo> list = baseMapper.selectUserCount();

        Map<String,Object> map = new HashMap<>();
        //创建两个list集合，一个对应所有日期，另外一个对应所有数据
        // list -- json数组 []
        //获取所有日期
        List<String> dateList =
                list.stream().map(UserCountVo::getRegisterDate).collect(Collectors.toList());
        //获取所有数据
        List<Integer> countList =
                list.stream().map(UserCountVo::getCount).collect(Collectors.toList());

        //放到map集合，返回
        map.put("dateList",dateList);
        map.put("countList",countList);
        return map;
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 用户总数
        stats.put("totalUsers", baseMapper.selectCount(null));

        // 今日新增用户数
        LambdaQueryWrapper<UserInfo> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.apply("DATE(create_time) = CURDATE()");
        stats.put("todayNewUsers", baseMapper.selectCount(todayWrapper));

        return stats;
    }

    @Override
    public String updateAvatar(MultipartFile file) {
        R<SysFile> result = remoteFileService.upload(file);
        if (result == null || result.getData() == null) {
            throw new ServiceException("头像上传失败");
        }
        String url = result.getData().getUrl();
        Long userId = SecurityUtils.getUserId();
        baseMapper.update(null, new LambdaUpdateWrapper<UserInfo>()
                .eq(UserInfo::getId, userId)
                .set(UserInfo::getAvatarUrl, url));
        return url;
    }

    @Override
    public void updateNickname(String nickname) {
        if (StringUtils.isEmpty(nickname)) {
            throw new ServiceException("昵称不能为空");
        }
        if (nickname.length() > 30) {
            throw new ServiceException("昵称长度不能超过30个字符");
        }
        Long userId = SecurityUtils.getUserId();
        baseMapper.update(null, new LambdaUpdateWrapper<UserInfo>()
                .eq(UserInfo::getId, userId)
                .set(UserInfo::getNickname, nickname));
    }

    @Override
    public UserInfo getCurrentUser() {
        Long userId = SecurityUtils.getUserId();
        return this.getById(userId);
    }

    @Override
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 用户总数
        stats.put("totalUsers", baseMapper.selectCount(null));

        // 今日新增
        LambdaQueryWrapper<UserInfo> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.apply("DATE(create_time) = CURDATE()");
        stats.put("todayNewUsers", baseMapper.selectCount(todayWrapper));

        // 本周新增
        LambdaQueryWrapper<UserInfo> weekWrapper = new LambdaQueryWrapper<>();
        weekWrapper.apply("YEARWEEK(create_time, 1) = YEARWEEK(CURDATE(), 1)");
        stats.put("weekNewUsers", baseMapper.selectCount(weekWrapper));

        // 本月新增
        LambdaQueryWrapper<UserInfo> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.apply("DATE_FORMAT(create_time, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m')");
        stats.put("monthNewUsers", baseMapper.selectCount(monthWrapper));

        // 活跃用户（最近30天有登录）
        stats.put("activeUsers", userInfoMapper.countActiveUsers());

        // 新增用户趋势
        stats.put("dailyTrend", userInfoMapper.selectNewUserTrend(null));

        return stats;
    }
}
