package com.share.goods.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.goods.domain.dto.SeckillOrderRequestDTO;
import com.share.goods.domain.vo.SeckillActivityVO;
import com.share.goods.domain.vo.SeckillDetailVO;
import com.share.goods.domain.vo.SeckillOrderResultVO;
import com.share.goods.service.ISeckillApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * C端秒杀接口 Controller
 *
 * @author share
 */
@Tag(name = "C端秒杀")
@RestController
@RequestMapping("/api/v1/seckill")
@RequiredArgsConstructor
public class SeckillApiController {

    private final ISeckillApiService seckillApiService;

    @Operation(summary = "秒杀活动列表")
    @GetMapping("/list")
    public R<List<SeckillActivityVO>> list() {
        return R.ok(seckillApiService.getSeckillList());
    }

    @Operation(summary = "秒杀活动详情")
    @GetMapping("/{id}")
    public R<SeckillDetailVO> detail(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        SeckillDetailVO vo = seckillApiService.getSeckillDetail(id, userId);
        if (vo == null) {
            return R.fail("活动不存在");
        }
        return R.ok(vo);
    }

    @Operation(summary = "参与秒杀")
    @RequiresLogin
    @SentinelResource(value = "seckillOrder", blockHandler = "seckillOrderBlockHandler")
    @PostMapping("/{id}/order")
    public R<SeckillOrderResultVO> createOrder(@PathVariable Long id,
                                                @Valid @RequestBody SeckillOrderRequestDTO dto) {
        Long userId = SecurityUtils.getUserId();
        String orderNo = seckillApiService.createSeckillOrder(id, userId, dto);
        return R.ok(new SeckillOrderResultVO(orderNo, null, dto.getQuantity(),
                BigDecimal.valueOf(dto.getQuantity()).multiply(BigDecimal.ONE)));
    }

    /** Sentinel 限流 fallback */
    public R<SeckillOrderResultVO> seckillOrderBlockHandler(Long id, SeckillOrderRequestDTO dto, BlockException e) {
        return R.fail("活动太火爆，请稍后再试");
    }
}
