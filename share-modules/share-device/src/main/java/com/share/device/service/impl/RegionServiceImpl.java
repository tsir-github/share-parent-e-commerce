package com.share.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.device.domain.Region;
import com.share.device.mapper.RegionMapper;
import com.share.device.service.IRegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class RegionServiceImpl extends ServiceImpl<RegionMapper, Region> implements IRegionService {
    @Autowired
    private RegionMapper regionMapper;
    @Override
    public List<Region> treeSelect(String code) {
        LambdaQueryWrapper<Region> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Region::getParentCode,code);
        List<Region> regionList = regionMapper.selectList(queryWrapper);
        //判断是否有下一层数据，如果有hasChildren=true,否则false
        if (!CollectionUtils.isEmpty(regionList)){
            //1.把region遍历，得到每个Region
            regionList.forEach(region -> {
                //2.查询每个Region对象是否有下一层数据parent_code=？
                LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Region::getParentCode,region.getCode());
                Long count = regionMapper.selectCount(wrapper);
                if (count > 0){
                    //如果有设置hasChildren=true,否则false
                    region.setHasChildren(true);
                }else {
                    region.setHasChildren(false);
                }
            });
        }

        return regionList;
        //return baseMapper.selectList(queryWrapper);
    }
}

