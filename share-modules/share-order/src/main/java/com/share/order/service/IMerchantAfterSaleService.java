package com.share.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.order.domain.AfterSaleRequest;

import java.util.List;

/**
 * 商家端售后 Service 接口
 *
 * @author share
 */
public interface IMerchantAfterSaleService extends IService<AfterSaleRequest> {

    /**
     * 查询商家售后列表
     */
    List<AfterSaleRequest> selectMerchantAfterSaleList(Long merchantId);

    /**
     * 商家同意退款
     */
    void approveAfterSale(Long id, Long merchantId);

    /**
     * 商家拒绝退款（转客服）
     */
    void rejectAfterSale(Long id, Long merchantId, String reason);
}
