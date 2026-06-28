package com.share.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.constant.MerchantStatus;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.merchant.domain.MerchantInfo;
import com.share.system.api.RemoteFileService;
import com.share.system.api.domain.SysFile;
import org.springframework.transaction.annotation.Transactional;
import com.share.merchant.domain.MerchantUser;
import com.share.merchant.mapper.MerchantInfoMapper;
import com.share.merchant.service.IMerchantInfoService;
import com.share.merchant.service.IMerchantUserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 商家信息 Service 实现
 *
 * @author share
 */
@Service
public class MerchantInfoServiceImpl extends ServiceImpl<MerchantInfoMapper, MerchantInfo> implements IMerchantInfoService {

    private final IMerchantUserService merchantUserService;
    private final RemoteFileService remoteFileService;

    public MerchantInfoServiceImpl(@Lazy IMerchantUserService merchantUserService, RemoteFileService remoteFileService) {
        this.merchantUserService = merchantUserService;
        this.remoteFileService = remoteFileService;
    }

    @Override
    public MerchantInfo getMerchantProfile(Long merchantId) {
        if (merchantId == null) {
            throw new ServiceException("未获取到商家信息");
        }
        MerchantInfo info = this.getById(merchantId);
        if (info == null) {
            throw new ServiceException("店铺不存在");
        }
        return info;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMerchantProfile(Long merchantId, MerchantInfo update) {
        if (merchantId == null) {
            throw new ServiceException("未获取到商家信息");
        }
        update.setId(merchantId);
        // 防止篡改敏感字段
        update.setUserId(null);
        update.setStatus(null);
        update.setAuditRemark(null);
        update.setAuditTime(null);
        this.updateById(update);
    }

    @Override
    public boolean checkMerchantStatus(Long merchantId) {
        MerchantInfo merchant = this.getById(merchantId);
        return merchant != null && MerchantStatus.ENABLED.equals(merchant.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(MerchantInfo merchantInfo) {
        MerchantInfo existing = this.getById(merchantInfo.getId());
        if (existing == null) {
            throw new ServiceException("商家不存在");
        }
        existing.setStatus(merchantInfo.getStatus());
        existing.setAuditRemark(merchantInfo.getAuditRemark());
        existing.setAuditTime(new Date());
        this.updateById(existing);

        // 审核通过 → 自动创建商家登录账号
        if (MerchantStatus.ENABLED.equals(merchantInfo.getStatus())) {
            MerchantUser user = merchantUserService.createMerchantUser(
                    existing.getId(), null, null);
            existing.setUserId(user.getId());
            this.updateById(existing);
        }
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 商家总数
        stats.put("totalMerchants", baseMapper.selectCount(null));

        // 待审核数（status=0）
        LambdaQueryWrapper<MerchantInfo> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(MerchantInfo::getStatus, MerchantStatus.PENDING_AUDIT);
        stats.put("pendingAudit", baseMapper.selectCount(pendingWrapper));

        // 已启用数（status=1）
        LambdaQueryWrapper<MerchantInfo> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(MerchantInfo::getStatus, MerchantStatus.ENABLED);
        stats.put("activeMerchants", baseMapper.selectCount(activeWrapper));

        return stats;
    }

    @Override
    public String updateLogo(Long merchantId, MultipartFile file) {
        if (merchantId == null) throw new ServiceException("未获取到商家信息");
        R<SysFile> result = remoteFileService.upload(file);
        if (result == null || result.getData() == null) {
            throw new ServiceException("图片上传失败");
        }
        String url = result.getData().getUrl();
        baseMapper.update(null, new LambdaUpdateWrapper<MerchantInfo>()
                .eq(MerchantInfo::getId, merchantId)
                .set(MerchantInfo::getLogo, url));
        return url;
    }
}
