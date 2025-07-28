package com.share.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.device.domain.Cabinet;
import com.share.device.mapper.CabinetMapper;
import com.share.device.service.ICabinetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CabinetServiceImpl extends ServiceImpl<CabinetMapper, Cabinet> implements ICabinetService {

    @Autowired
    private CabinetMapper cabinetMapper;
    @Override
    public List<Cabinet> selectCabinetList(Cabinet cabinet) {
        return cabinetMapper.selectCabinetList(cabinet);
    }

    //查询未使用的柜机
    @Override
    public List<Cabinet> searchNoUseList(String keyword) {
        LambdaQueryWrapper<Cabinet> cabinetLambdaQueryWrapper = new LambdaQueryWrapper<>();
        cabinetLambdaQueryWrapper.like(Cabinet::getCabinetNo,keyword);
        cabinetLambdaQueryWrapper.eq(Cabinet::getStatus,0);
        List<Cabinet> list = cabinetMapper.selectList(cabinetLambdaQueryWrapper);
        return list;
    }
}
