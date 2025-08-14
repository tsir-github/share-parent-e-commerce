package com.share.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.device.domain.Cabinet;
import com.share.device.domain.CabinetSlot;
import com.share.device.domain.PowerBank;
import com.share.device.mapper.CabinetMapper;
import com.share.device.mapper.CabinetSlotMapper;
import com.share.device.service.ICabinetService;
import com.share.device.service.IPowerBankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CabinetServiceImpl extends ServiceImpl<CabinetMapper, Cabinet> implements ICabinetService {

    @Autowired
    private CabinetMapper cabinetMapper;
    @Autowired
    private CabinetSlotMapper cabinetSlotMapper;

    @Autowired
    private IPowerBankService powerBankService;
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

    @Override
    public Map<String, Object> getAllInfo(Long id) {
        // 查询柜机信息.调用Service层的 getById(id)方法，通过主键查询对应的 Cabinet实体对象
        Cabinet cabinet = this.getById(id);//
        // 查询插槽信息
        //List<CabinetSlot> cabinetSlotList = cabinetSlotMapper.selectList(new LambdaQueryWrapper<CabinetSlot>().eq(CabinetSlot::getCabinetId, cabinet.getId()));
        LambdaQueryWrapper<CabinetSlot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CabinetSlot::getCabinetId,cabinet.getId());//查找所有 cabinet_id等于当前柜机ID的插槽。
        List<CabinetSlot> cabinetSlotList = cabinetSlotMapper.selectList(queryWrapper);//返回该柜机的插槽列表 cabinetSlotList
        //这段代码的作用是从柜机（Cabinet）中获取所有插槽（CabinetSlot），并为每个插槽填充对应的充电宝（PowerBank）信息。
        List<Long> powerBankIdList = cabinetSlotList.stream()
                .filter(item -> null != item.getPowerBankId())  //通过 filter排除没有充电宝的插槽（powerBankId为 null）。
                .map(CabinetSlot::getPowerBankId)   //通过 map收集所有非空插槽的 powerBankId。
                .collect(Collectors.toList());

        if(!CollectionUtils.isEmpty(powerBankIdList)) {
            //若 powerBankIdList非空，调用 powerBankService.listByIds()批量查询所有关联的充电宝对象。
            List<PowerBank> powerBankList = powerBankService.listByIds(powerBankIdList);
            //将充电宝列表转换为 Map<Long, PowerBank>（Key=充电宝ID，Value=充电宝对象），便于快速查找。
            Map<Long,PowerBank> powerBankIdToPowerBankMap = powerBankList.stream()
                    .collect(Collectors.toMap(
                            PowerBank::getId,   // Key映射函数
                            PowerBank -> PowerBank // Value映射函数
                    ));
            //通过 item.getPowerBankId()获取当前插槽的充电宝ID。从 powerBankIdToPowerBankMap中查找对应的 PowerBank对象。
            //调用 setPowerBank()将充电宝对象注入插槽的扩展属性中（@TableField(exist = false)字段）。
            cabinetSlotList.forEach(
                    item -> item.setPowerBank(
                            powerBankIdToPowerBankMap.get(item.getPowerBankId())
                    )
            );
        }
        //"cabinet"：柜机基础信息对象。"cabinetSlotList"：包含完整充电宝详情的插槽列表
        Map<String, Object> result = Map.of("cabinet", cabinet, "cabinetSlotList", cabinetSlotList);

        return result;
    }


}
