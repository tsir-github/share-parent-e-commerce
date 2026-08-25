package com.share.goods.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.InnerAuth;
import com.share.goods.service.ISeckillApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 秒杀内部 Feign Controller
 *
 * @author share
 */
@Tag(name = "秒杀内部接口")
@RestController
@RequestMapping("/inner/seckill")
@RequiredArgsConstructor
public class InnerSeckillController {

    private final ISeckillApiService seckillApiService;

    @Operation(summary = "归还秒杀库存（内部）")
    @InnerAuth
    @PostMapping("/releaseStock/{activityId}")
    public R<Void> releaseStock(@PathVariable Long activityId, @RequestParam int quantity) {
        seckillApiService.releaseStock(activityId, quantity);
        return R.ok();
    }

    @Operation(summary = "根据订单号归还秒杀库存（超时取消，内部）")
    @InnerAuth
    @PostMapping("/releaseStockByOrderNo/{orderNo}")
    public R<Void> releaseStockByOrderNo(@PathVariable String orderNo) {
        seckillApiService.releaseStockByOrderNo(orderNo);
        return R.ok();
    }
}
