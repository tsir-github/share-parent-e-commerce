package com.share.rules.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.rules.domain.FeeRule;
import com.share.rules.mapper.FeeRuleMapper;
import com.share.rules.service.IFeeRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class FeeRuleServiceImpl extends ServiceImpl<FeeRuleMapper, FeeRule> implements IFeeRuleService {

    @Autowired
    private FeeRuleMapper feeRuleMapper;

    @Override
    public List<FeeRule> selectFeeRuleList(FeeRule feeRule) {
        return feeRuleMapper.selectFeeRuleList(feeRule);
    }

    //用于关联查询站点时，把费用规则也查询出来,批量查询
    @Override
    public List<FeeRule> selectByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()){
            return Collections.emptyList();// 查询结果列表，如果输入为空则返回空列表
        }
        List<FeeRule> list = feeRuleMapper.selectBatchIds(ids);
        return list;
    }

    @Override
    public List<FeeRule> getALLFeeRuleList() {
        LambdaQueryWrapper<FeeRule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FeeRule::getStatus,"1");
        List<FeeRule> list = feeRuleMapper.selectList(queryWrapper);
        return list;
    }
}
