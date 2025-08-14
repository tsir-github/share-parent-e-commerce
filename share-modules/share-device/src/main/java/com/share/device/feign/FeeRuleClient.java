package com.share.device.feign;


import com.share.common.core.domain.R;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.device.domain.dto.FeeRuleDTO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;



/**
 * 费用规则服务Feign客户端
 *
 * @FeignClient 注解说明:
 *   value/name: 指定要调用的服务名(对应规则模块的spring.application.name)
 *   contextId: 当有多个Feign客户端时用于区分
 *   path: 统一前缀路径
 */
@FeignClient(
        value = "share-rule",
        contextId = "feeRuleClient",
        path = "/feeRule"
)
public interface FeeRuleClient {
    /**
     * 批量查询费用规则
     *
     * @param ids 规则ID集合
     * @return 查询结果
     */
    @PostMapping("/batchGet")
    AjaxResult batchGet(@RequestBody List<Long> ids);


    /**
     * 查询费用规则列表
     *
     * @param feeRule 查询条件
     * @return 分页结果
     *
     * 注意：GET请求传递对象参数时，需要使用@SpringQueryMap注解
     */
    @GetMapping("/list")
    TableDataInfo list(@SpringQueryMap FeeRuleDTO feeRule);


    /**
     * 获取费用规则详细信息
     *
     * @param id 规则ID
     * @return 规则详情
     */
    @GetMapping("/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 新增费用规则
     *
     * @param feeRule 规则对象
     * @return 操作结果
     */
    @PostMapping
    AjaxResult add(@RequestBody FeeRuleDTO feeRule);

    /**
     * 批量添加费用规则
     *
     * @param feeRules 规则列表
     * @return 操作结果
     */
    @PostMapping("/batch")
    AjaxResult addBatch(@RequestBody List<FeeRuleDTO> feeRules);

    /**
     * 修改费用规则
     *
     * @param feeRule 规则对象
     * @return 操作结果
     */
    @PutMapping
    AjaxResult edit(@RequestBody FeeRuleDTO feeRule);

    /**
     * 删除费用规则
     *
     * @param ids 规则ID数组
     * @return 操作结果
     */
    @DeleteMapping("/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 获取全部有效费用规则
     *
     * @return 规则列表
     */
    @GetMapping("/getALLFeeRuleList")
    AjaxResult getALLFeeRuleList();
}