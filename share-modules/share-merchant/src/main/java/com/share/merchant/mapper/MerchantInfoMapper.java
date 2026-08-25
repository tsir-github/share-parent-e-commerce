package com.share.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.merchant.domain.MerchantInfo;
import com.share.merchant.domain.vo.MerchantInfoVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商家信息 Mapper 接口
 *
 * @author share
 */
public interface MerchantInfoMapper extends BaseMapper<MerchantInfo> {

    /**
     * 查询商家列表（含关联账号信息）
     */
    List<MerchantInfoVO> selectListWithAccount(@Param("name") String name,
                                                @Param("status") String status);
}
