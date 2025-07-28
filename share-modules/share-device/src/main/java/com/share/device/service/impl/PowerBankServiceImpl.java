package com.share.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.exception.ServiceException;
import com.share.device.domain.PowerBank;
import com.share.device.mapper.PowerBankMapper;
import com.share.device.service.IPowerBankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PowerBankServiceImpl extends ServiceImpl<PowerBankMapper, PowerBank> implements IPowerBankService {

    @Autowired
    private PowerBankMapper powerBankMapper;
    @Override
    public List<PowerBank> selectPowerBankList(PowerBank powerBank) {

        return powerBankMapper.selectPowerBankList(powerBank);
    }

    /**
     * 保存充电宝信息
     * @param powerBank 充电宝对象，包含充电宝的详细信息
     * @return 返回影响的行数，1表示保存成功，0表示保存失败
     * @throws ServiceException 当充电宝编号已存在时抛出业务异常
     */
    @Override
    public int savePowerBank(PowerBank powerBank) {
        //1,判断充电宝编号powerBankNo是否存在，如果存在不进行添加，否则进行添加
        String powerBankNo = powerBank.getPowerBankNo();
        // 构造查询条件，根据充电宝编号查询是否存在相同记录
        LambdaQueryWrapper<PowerBank> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PowerBank::getPowerBankNo,powerBankNo);
        //selectCount和selectOne都可以
        Long count = powerBankMapper.selectCount(queryWrapper);
        if(count > 0){
            throw new ServiceException("充电宝编号以存在");
        }
        int rows = powerBankMapper.insert(powerBank);
        return rows;
    }

    @Override
    public int updatePowerBank(PowerBank powerBank) {
        LambdaUpdateWrapper<PowerBank> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PowerBank::getId,powerBank.getId())
                .eq(PowerBank::getStatus,"0")
                .set(PowerBank::getPowerBankNo,powerBank.getPowerBankNo())
                .set(PowerBank::getElectricity,powerBank.getElectricity())
                .set(PowerBank::getDescription, powerBank.getDescription())
                .set(PowerBank::getStatus, powerBank.getStatus());
        return powerBankMapper.update(null,updateWrapper);
        //判断状态是0（未投放），才做修改
        /*Long id = powerBank.getId();

        PowerBank oldPowerBank = powerBankMapper.selectById(id);
        if (oldPowerBank.getStatus().equals("0") && oldPowerBank!=null){
            return powerBankMapper.updateById(powerBank);
        }
        return 0;*/
    }
}
