package com.share.device.feign.fallback;

import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.device.domain.dto.FeeRuleDTO;
import com.share.device.feign.FeeRuleClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class FeeRuleClientFallback implements FeeRuleClient {
    @Override
    public AjaxResult batchGet(List<Long> ids) {
        log.error("Feign调用失败: 批量获取费用规则降级, ids={}", ids);
        return AjaxResult.error("规则服务不可用");
    }
    @Override
    public TableDataInfo list(FeeRuleDTO feeRule) {
        return null;
    }

    @Override
    public AjaxResult getInfo(Long id) {
        return null;
    }

    @Override
    public AjaxResult add(FeeRuleDTO feeRule) {
        return null;
    }

    @Override
    public AjaxResult addBatch(List<FeeRuleDTO> feeRules) {
        return null;
    }

    @Override
    public AjaxResult edit(FeeRuleDTO feeRule) {
        return null;
    }

    @Override
    public AjaxResult remove(Long[] ids) {
        return null;
    }

    @Override
    public AjaxResult getALLFeeRuleList() {
        return null;
    }
}
