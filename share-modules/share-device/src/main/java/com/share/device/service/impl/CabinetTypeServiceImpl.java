package com.share.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.device.domain.CabinetType;
import com.share.device.mapper.CabinetTypeMapper;
import com.share.device.service.ICabinetTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CabinetTypeServiceImpl extends ServiceImpl<CabinetTypeMapper, CabinetType> implements ICabinetTypeService{

    @Autowired
    CabinetTypeMapper cabinetTypeMapper;
    @Override
    public List<CabinetType> selectCabinetTypeList(CabinetType cabinetType) {
        //LambdaQueryWrapper<CabinetType> queryWrapper = new LambdaQueryWrapper<>();
        //queryWrapper.eq(CabinetType::getStatus,cabinetType.getStatus());
        return cabinetTypeMapper.selectCabinetTypeList(cabinetType);
    }
}