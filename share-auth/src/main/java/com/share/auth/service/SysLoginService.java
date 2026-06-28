package com.share.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.share.common.core.constant.CacheConstants;
import com.share.common.core.constant.Constants;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.constant.UserConstants;
import com.share.common.core.domain.R;
import com.share.common.core.enums.UserStatus;
import com.share.common.core.exception.ServiceException;
import com.share.common.core.text.Convert;
import com.share.common.core.utils.StringUtils;
import com.share.common.core.utils.ip.IpUtils;
import com.share.common.redis.service.RedisService;
import com.share.common.security.utils.SecurityUtils;
import com.share.system.api.RemoteUserService;
import com.share.system.api.domain.SysUser;
import com.share.system.api.model.LoginUser;

/**
 * 登录校验方法
 *
 * TokenController.login() 调用的第一步。
 * 负责：校验参数 → 查数据库 → 对比密码 → 返回 LoginUser。
 *
 * 校验通过后返回的 LoginUser 会交给 TokenService.createToken()，
 * 最终存到 Redis + 生成 JWT。
 *
 * @author share
 */
@Component
public class SysLoginService
{
    @Autowired
    private RemoteUserService remoteUserService;
    // Feign 客户端，远程调用 share-system 服务从数据库查用户

    @Autowired
    private SysPasswordService passwordService;
    // 密码校验服务（含错误次数锁定逻辑）

    @Autowired
    private SysRecordLogService recordLogService;
    // 记录登录日志到 sys_logininfor 表

    @Autowired
    private RedisService redisService;
    // 用于读取系统配置（IP 黑名单）

    /**
     * ★ 执行登录校验（核心方法）
     *
     * 按顺序做 7 件事：
     *   1. 参数空值校验
     *   2. 参数长度校验
     *   3. IP 黑名单检查
     *   4. Feign 调 system 服务查数据库
     *   5. 检查用户状态（删除/停用）
     *   6. BCrypt 对比密码
     *   7. 记录登录日志
     *
     * @param username 用户名（如 "admin"）
     * @param password 明文密码（如 "admin123"）
     * @return LoginUser（含用户信息、角色、权限）
     */
    public LoginUser login(String username, String password)
    {
        // ========== 第 1 步：空值校验 ==========
        if (StringUtils.isAnyBlank(username, password))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户/密码必须填写");
            throw new ServiceException("用户/密码必须填写");
        }

        // ========== 第 2 步：长度校验 ==========
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户密码不在指定范围");
            throw new ServiceException("用户密码不在指定范围");
        }
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户名不在指定范围");
            throw new ServiceException("用户名不在指定范围");
        }

        // ========== 第 3 步：IP 黑名单校验 ==========
        // Nacos 配置 sys.login.blackIPList 中设置的黑名单 IP
        String blackStr = Convert.toStr(redisService.getCacheObject(CacheConstants.SYS_LOGIN_BLACKIPLIST));
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "很遗憾，访问IP已被列入系统黑名单");
            throw new ServiceException("很遗憾，访问IP已被列入系统黑名单");
        }

        // ========== 第 4 步：Feign 调 system 服务查数据库 ==========
        // remoteUserService.getUserInfo 实际发起的 HTTP 请求：
        //   GET http://share-system/system/user/getInfo?username=admin
        // SecurityConstants.INNER 是内部调用标识，防止外部非法请求
        R<LoginUser> userResult = remoteUserService.getUserInfo(username, SecurityConstants.INNER);

        // ========== 第 5 步：检查用户状态 ==========
        if (StringUtils.isNull(userResult) || StringUtils.isNull(userResult.getData()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "登录用户不存在");
            throw new ServiceException("登录用户：" + username + " 不存在");
        }

        if (R.FAIL == userResult.getCode())
        {
            throw new ServiceException(userResult.getMsg());
        }

        // ★ 拿到完整的用户信息
        LoginUser userInfo = userResult.getData();
        SysUser user = userResult.getData().getSysUser();
        // LoginUser 包含（后面会原样存到 Redis）：
        //   userid: 1
        //   username: "admin"
        //   sysUser: { userId:1, userName:"admin", password:"$2a$10$...", status:"0", ... }
        //   roles: ["admin"]
        //   permissions: ["*:*:*"]

        if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "对不起，您的账号已被删除");
            throw new ServiceException("对不起，您的账号：" + username + " 已被删除");
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            recordLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户已停用，请联系管理员");
            throw new ServiceException("对不起，您的账号：" + username + " 已停用");
        }

        // ========== 第 6 步：BCrypt 密码校验 ==========
        // 内部逻辑：
        //   - 从 Redis 读取密码错误次数（key = "pwd_err_cnt:admin"）
        //   - 如果错误次数 >= 5，锁定 10 分钟
        //   - 用 BCryptPasswordEncoder.matches(password, dbPassword) 对比
        //   - 成功则清空错误次数，失败则 +1
        passwordService.validate(userInfo, password);

        // ========== 第 7 步：记录登录成功日志 ==========
        recordLogService.recordLogininfor(username, Constants.LOGIN_SUCCESS, "登录成功");

        // ★ 返回 LoginUser 给 TokenController
        return userInfo;
    }

    public void logout(String loginName)
    {
        recordLogService.recordLogininfor(loginName, Constants.LOGOUT, "退出成功");
    }

    /**
     * 注册
     */
    public void register(String username, String password)
    {
        // 用户名或密码为空 错误
        if (StringUtils.isAnyBlank(username, password))
        {
            throw new ServiceException("用户/密码必须填写");
        }
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            throw new ServiceException("账户长度必须在2到20个字符之间");
        }
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            throw new ServiceException("密码长度必须在5到20个字符之间");
        }

        // 注册用户信息
        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);
        sysUser.setNickName(username);
        sysUser.setPassword(SecurityUtils.encryptPassword(password));
        R<?> registerResult = remoteUserService.registerUserInfo(sysUser, SecurityConstants.INNER);

        if (R.FAIL == registerResult.getCode())
        {
            throw new ServiceException(registerResult.getMsg());
        }
        recordLogService.recordLogininfor(username, Constants.REGISTER, "注册成功");
    }
}
