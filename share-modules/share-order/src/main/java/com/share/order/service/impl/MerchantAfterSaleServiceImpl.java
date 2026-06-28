package com.share.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.exception.ServiceException;
import com.share.order.domain.AfterSaleRequest;
import com.share.order.mapper.AfterSaleRequestMapper;
import com.share.order.service.IAfterSaleRequestService;
import com.share.order.service.IMerchantAfterSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商家端售后 Service 实现
 *
 * @author share
 */
@Service
@RequiredArgsConstructor
public class MerchantAfterSaleServiceImpl extends ServiceImpl<AfterSaleRequestMapper, AfterSaleRequest> implements IMerchantAfterSaleService {

    private final IAfterSaleRequestService afterSaleRequestService;

    @Override
    public List<AfterSaleRequest> selectMerchantAfterSaleList(Long merchantId) {
        if (merchantId == null) {
            throw new ServiceException("未获取到商家信息");
        }
        return afterSaleRequestService.selectByMerchantId(merchantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveAfterSale(Long id, Long merchantId) {
        if (merchantId == null) {
            throw new ServiceException("未获取到商家信息");
        }
        AfterSaleRequest req = afterSaleRequestService.getById(id);
        if (req == null) {
            throw new ServiceException("售后申请不存在");
        }
        if (!merchantId.equals(req.getMerchantId())) {
            throw new ServiceException("无权操作");
        }
        afterSaleRequestService.audit(id, "1", "商家同意退款");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectAfterSale(Long id, Long merchantId, String reason) {
        if (merchantId == null) {
            throw new ServiceException("未获取到商家信息");
        }
        AfterSaleRequest req = afterSaleRequestService.getById(id);
        if (req == null) {
            throw new ServiceException("售后申请不存在");
        }
        if (!merchantId.equals(req.getMerchantId())) {
            throw new ServiceException("无权操作");
        }
        afterSaleRequestService.audit(id, "2", reason != null ? reason : "商家拒绝退款");
    }
}
