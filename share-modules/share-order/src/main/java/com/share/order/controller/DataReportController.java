package com.share.order.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.security.annotation.RequiresPermissions;
import com.share.order.service.IDataReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台数据报表 Controller（管理员端）
 *
 * <p>全部为 GET 只读查询，返回 {@link AjaxResult}。</p>
 *
 * @author share
 */
@Tag(name = "平台数据报表")
@RestController
@RequestMapping("/data-report")
@RequiredArgsConstructor
public class DataReportController extends BaseController {

    private final IDataReportService dataReportService;

    @Operation(summary = "交易总览")
    @RequiresPermissions("order:report:overview")
    @GetMapping("/overview")
    public AjaxResult overview(@RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate) {
        return success(dataReportService.getOverview(startDate, endDate));
    }

    @Operation(summary = "订单趋势")
    @RequiresPermissions("order:report:trend")
    @GetMapping("/trend")
    public AjaxResult trend(@RequestParam(required = false) String startDate,
                            @RequestParam(required = false) String endDate) {
        return success(dataReportService.getTrend(startDate, endDate));
    }

    @Operation(summary = "商品销售排行")
    @RequiresPermissions("order:report:productRanking")
    @GetMapping("/product-ranking")
    public AjaxResult productRanking(@RequestParam(required = false) String startDate,
                                     @RequestParam(required = false) String endDate,
                                     @RequestParam(required = false, defaultValue = "salesCount") String sortBy,
                                     @RequestParam(required = false, defaultValue = "20") Integer topN) {
        return success(dataReportService.getProductRanking(startDate, endDate, sortBy, topN));
    }

    @Operation(summary = "商家销售排行")
    @RequiresPermissions("order:report:merchantRanking")
    @GetMapping("/merchant-ranking")
    public AjaxResult merchantRanking(@RequestParam(required = false) String startDate,
                                      @RequestParam(required = false) String endDate,
                                      @RequestParam(required = false, defaultValue = "20") Integer topN) {
        return success(dataReportService.getMerchantRanking(startDate, endDate, topN));
    }

    @Operation(summary = "支付统计")
    @RequiresPermissions("order:report:paymentStats")
    @GetMapping("/payment-stats")
    public AjaxResult paymentStats(@RequestParam(required = false) String startDate,
                                   @RequestParam(required = false) String endDate) {
        return success(dataReportService.getPaymentStats(startDate, endDate));
    }
}
