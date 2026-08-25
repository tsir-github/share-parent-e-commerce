<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="交易总览" name="overview">
        <el-row :gutter="20" style="margin-top:16px">
          <el-col :span="6" v-for="card in overviewCards" :key="card.label">
            <el-card shadow="hover" class="report-card">
              <div class="card-label">{{ card.label }}</div>
              <div class="card-value">{{ card.value }}</div>
            </el-card>
          </el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top:16px">
          <el-col :span="8" v-for="card in overviewPeriods" :key="card.label">
            <el-card shadow="hover">
              <div class="card-label">{{ card.label }}</div>
              <div class="card-sub">
                <span>订单: <b>{{ card.orders }}</b></span>
                <span style="margin-left:16px">交易额: <b>¥{{ card.revenue }}</b></span>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="订单趋势" name="trend">
        <el-form :inline="true" style="margin-top:16px">
          <el-form-item label="日期">
            <el-date-picker v-model="trendDateRange" type="daterange" range-separator="~" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" @change="loadTrend" />
          </el-form-item>
        </el-form>
        <div ref="trendChart" style="width:100%;height:400px"></div>
      </el-tab-pane>

      <el-tab-pane label="商品排行" name="productRanking">
        <el-form :inline="true" style="margin-top:16px">
          <el-form-item label="排序">
            <el-select v-model="productSortBy" @change="loadProductRanking" style="width:120px">
              <el-option label="按销量" value="salesCount" />
              <el-option label="按交易额" value="revenue" />
            </el-select>
          </el-form-item>
          <el-form-item label="Top N">
            <el-input-number v-model="productTopN" :min="5" :max="100" @change="loadProductRanking" />
          </el-form-item>
        </el-form>
        <el-table :data="productRanking" border stripe v-loading="productLoading" max-height="500">
          <el-table-column type="index" label="排名" width="60" align="center" />
          <el-table-column label="商品名称" prop="productName" min-width="160" />
          <el-table-column label="销量" prop="salesCount" width="80" align="center" />
          <el-table-column label="交易额" prop="revenue" width="120" align="center">
            <template #default="s">¥{{ s.row.revenue }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="商家排行" name="merchantRanking">
        <el-form :inline="true" style="margin-top:16px">
          <el-form-item label="Top N">
            <el-input-number v-model="merchantTopN" :min="5" :max="100" @change="loadMerchantRanking" />
          </el-form-item>
        </el-form>
        <el-table :data="merchantRanking" border stripe v-loading="merchantLoading" max-height="500">
          <el-table-column type="index" label="排名" width="60" align="center" />
          <el-table-column label="商家名称" prop="merchantName" min-width="160" />
          <el-table-column label="订单数" prop="orderCount" width="80" align="center" />
          <el-table-column label="交易额" prop="revenue" width="120" align="center">
            <template #default="s">¥{{ s.row.revenue }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="支付统计" name="paymentStats">
        <el-table :data="paymentStats" border stripe v-loading="paymentLoading" style="margin-top:16px" max-height="500">
          <el-table-column label="支付方式" prop="payWayName" width="120" />
          <el-table-column label="订单数" prop="orderCount" width="80" align="center" />
          <el-table-column label="金额" prop="amount" width="120" align="center">
            <template #default="s">¥{{ s.row.amount }}</template>
          </el-table-column>
          <el-table-column label="占比" prop="ratio" width="100" align="center" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="DataReport">
import { ref, onMounted, nextTick } from 'vue'
import { getOverview, getTrend, getProductRanking, getMerchantRanking, getPaymentStats } from '@/api/order/dataReport'
import * as echarts from 'echarts'

const activeTab = ref('overview')

const overviewCards = ref([])
const overviewPeriods = ref([])
const trendDateRange = ref([])
const productSortBy = ref('salesCount')
const productTopN = ref(20)
const merchantTopN = ref(20)
const productRanking = ref([])
const productLoading = ref(false)
const merchantRanking = ref([])
const merchantLoading = ref(false)
const paymentStats = ref([])
const paymentLoading = ref(false)

function loadOverview() {
  getOverview().then(res => {
    const d = res.data || {}
    overviewCards.value = [
      { label: '今日订单', value: d.todayOrderCount ?? '-' },
      { label: '今日交易额', value: '¥' + (d.todayRevenue || 0) },
      { label: '今日客单价', value: '¥' + (d.todayAvgOrderAmount || 0) },
      { label: '今日退款', value: '¥' + (d.todayRefundAmount || 0) }
    ]
    overviewPeriods.value = [
      { label: '本周', orders: d.weekOrderCount || 0, revenue: d.weekRevenue || 0 },
      { label: '本月', orders: d.monthOrderCount || 0, revenue: d.monthRevenue || 0 },
      { label: '累计', orders: d.totalOrders || 0, revenue: d.totalRevenue || 0 }
    ]
  })
}

function loadTrend() {
  const params = {}
  if (trendDateRange.value && trendDateRange.value.length === 2) {
    params.startDate = trendDateRange.value[0]
    params.endDate = trendDateRange.value[1]
  }
  getTrend(params).then(res => {
    const data = res.data || []
    nextTick(() => {
      const chart = echarts.init(document.querySelector('.el-tab-pane#pane-trend .app-container') || document.body)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: '5%', right: '5%', bottom: '10%' },
        xAxis: { type: 'category', data: data.map(d => d.date), axisLabel: { rotate: 45 } },
        yAxis: [
          { type: 'value', name: '订单数' },
          { type: 'value', name: '交易额' }
        ],
        series: [
          { name: '订单数', type: 'bar', data: data.map(d => d.orderCount) },
          { name: '交易额', type: 'line', yAxisIndex: 1, data: data.map(d => d.revenue) }
        ]
      })
    })
  })
}

function loadProductRanking() {
  productLoading.value = true
  getProductRanking({ sortBy: productSortBy.value, topN: productTopN.value }).then(res => {
    productRanking.value = res.data || []
    productLoading.value = false
  })
}

function loadMerchantRanking() {
  merchantLoading.value = true
  getMerchantRanking({ topN: merchantTopN.value }).then(res => {
    merchantRanking.value = res.data || []
    merchantLoading.value = false
  })
}

function loadPaymentStats() {
  paymentLoading.value = true
  getPaymentStats().then(res => {
    paymentStats.value = res.data || []
    paymentLoading.value = false
  })
}

function onTabChange(tab) {
  if (tab === 'trend') loadTrend()
  else if (tab === 'productRanking') loadProductRanking()
  else if (tab === 'merchantRanking') loadMerchantRanking()
  else if (tab === 'paymentStats') loadPaymentStats()
}

onMounted(() => { loadOverview() })
</script>

<style scoped>
.report-card .card-label { font-size: 13px; color: #999; }
.report-card .card-value { font-size: 26px; font-weight: 700; margin-top: 8px; color: #303133; }
.card-sub { font-size: 14px; color: #666; margin-top: 6px; }
.card-sub b { color: #303133; }
</style>
