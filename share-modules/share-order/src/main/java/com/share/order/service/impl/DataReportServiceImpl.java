package com.share.order.service.impl;

import com.share.order.mapper.DataReportMapper;
import com.share.order.service.IDataReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 平台数据报表 Service 实现
 *
 * <p>所有数据通过 {@link DataReportMapper} 跨库聚合查询，无写操作。</p>
 *
 * @author share
 */
@RequiredArgsConstructor
@Service
public class DataReportServiceImpl implements IDataReportService {

    private final DataReportMapper dataReportMapper;

    @Override
    public Map<String, Object> getOverview(String startDate, String endDate) {
        // ponytail: date range params reserved for future custom range; v1 returns fixed dimensions
        return dataReportMapper.selectOverview();
    }

    @Override
    public List<Map<String, Object>> getTrend(String startDate, String endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(29).format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (endDate == null) {
            endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        List<Map<String, Object>> dbResult = dataReportMapper.selectTrend(startDate, endDate);

        // 填充无订单日期的空值，保证日期连续
        // ponytail: O(n) scan for gap-filling, switch to SQL calendar table if perf matters
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        Map<String, Map<String, Object>> dateMap = new TreeMap<>();
        for (Map<String, Object> row : dbResult) {
            dateMap.put((String) row.get("date"), row);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            String key = d.format(DateTimeFormatter.ISO_LOCAL_DATE);
            if (dateMap.containsKey(key)) {
                result.add(dateMap.get(key));
            } else {
                result.add(Map.of("date", key, "orderCount", 0, "revenue", 0));
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getProductRanking(String startDate, String endDate, String sortBy, Integer topN) {
        if (topN == null || topN <= 0) {
            topN = 20;
        }
        if (topN > 100) {
            topN = 100;
        }
        List<Map<String, Object>> list = dataReportMapper.selectProductRanking(startDate, endDate, sortBy, topN);
        // 填充排名序号
        int rank = 1;
        for (Map<String, Object> row : list) {
            row.put("rank", rank++);
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> getMerchantRanking(String startDate, String endDate, Integer topN) {
        if (topN == null || topN <= 0) {
            topN = 20;
        }
        if (topN > 100) {
            topN = 100;
        }
        List<Map<String, Object>> list = dataReportMapper.selectMerchantRanking(startDate, endDate, topN);
        int rank = 1;
        for (Map<String, Object> row : list) {
            row.put("rank", rank++);
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> getPaymentStats(String startDate, String endDate) {
        List<Map<String, Object>> list = dataReportMapper.selectPaymentStats(startDate, endDate);

        // 计算占比
        double totalAmount = list.stream()
                .mapToDouble(r -> ((Number) r.get("amount")).doubleValue())
                .sum();
        for (Map<String, Object> row : list) {
            double ratio = totalAmount > 0 ? ((Number) row.get("amount")).doubleValue() / totalAmount * 100 : 0;
            row.put("ratio", String.format("%.2f%%", ratio));
        }
        return list;
    }
}
