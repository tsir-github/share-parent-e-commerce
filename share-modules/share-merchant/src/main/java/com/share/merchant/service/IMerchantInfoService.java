package com.share.merchant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.merchant.domain.MerchantInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 商家信息 Service 接口
 *
 * @author share
 */
public interface IMerchantInfoService extends IService<MerchantInfo> {

    /**
     * 检查商家是否启用
     */
    boolean checkMerchantStatus(Long merchantId);

    /**
     * 审核商家
     *
     * @param merchantInfo 审核信息（含 id、status、auditRemark）
     */
    void audit(MerchantInfo merchantInfo);

    /**
     * 获取商家店铺资料（含空值校验）
     *
     * @param merchantId 商家ID
     * @return 商家信息
     */
    MerchantInfo getMerchantProfile(Long merchantId);

    /**
     * 更新商家店铺资料（防篡改敏感字段）
     *
     * @param merchantId 商家ID
     * @param update     待更新字段
     */
    void updateMerchantProfile(Long merchantId, MerchantInfo update);

    /**
     * 统计商家仪表盘数据
     *
     * @return 包含 totalMerchants, pendingAudit, activeMerchants 的 Map
     */
    Map<String, Object> getDashboardStats();

    /**
     * 更新店铺Logo
     *
     * @param merchantId 商家ID
     * @param file       图片文件
     * @return 新的Logo URL
     */
    String updateLogo(Long merchantId, MultipartFile file);
}
