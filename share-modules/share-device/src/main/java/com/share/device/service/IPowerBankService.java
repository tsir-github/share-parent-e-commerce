package com.share.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.device.domain.PowerBank;

import java.util.List;

public interface IPowerBankService extends IService<PowerBank> {

    List<PowerBank> selectPowerBankList(PowerBank powerBank);

    int savePowerBank(PowerBank powerBank);

    int updatePowerBank(PowerBank powerBank);
}
