package com.share.merchant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.merchant.domain.MerchantInfo;
import com.share.merchant.domain.MerchantUser;
import com.share.merchant.domain.vo.MerchantInfoVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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
     * @return 创建的商家用户（审核通过时），否则 null
     */
    MerchantUser audit(MerchantInfo merchantInfo);

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

    /**
     * 通过URL更新店铺Logo（前端先上传到文件服务后传URL）
     */
    void updateLogoUrl(Long merchantId, String logoUrl);

    /**
     * 查询商家列表（含关联账号信息）
     */
    List<MerchantInfoVO> selectListWithAccount(String name, String status);

    /**
     * 更新商家状态
     */
    void updateStatus(Long id, String status);

    /**
     * 管理员编辑商家基本信息（只更新编辑相关字段，不碰状态）
     */
    void adminUpdate(MerchantInfo merchantInfo);
}
