package com.share.user.factory;

import com.share.common.core.domain.R;
import com.share.user.api.RemoteUserService;
import com.share.user.domain.dto.WxLoginResultDTO;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 用户服务降级处理
 *
 * @author share
 */
@Component("remoteUserFallbackFactoryUser")
public class RemoteUserFallbackFactory implements FallbackFactory<RemoteUserService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteUserFallbackFactory.class);

    @Override
    public RemoteUserService create(Throwable throwable)
    {
        log.error("用户服务调用失败:{}", throwable.getMessage());
        return new RemoteUserService()
        {
            @Override
            public R<WxLoginResultDTO> wxLogin(String code)
            {
                return R.fail("微信登录失败:" + throwable.getMessage());
            }

            @Override
            public R<com.share.user.domain.UserInfo> getInfo(Long id)
            {
                return R.fail("获取用户信息失败:" + throwable.getMessage());
            }

            @Override
            public R<Map<String,Object>> getUserCount()
            {
                return R.fail("获取用户统计失败:" + throwable.getMessage());
            }
        };
    }
}
