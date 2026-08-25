<template>
  <div class="app-container">
    <div class="page-header"><h2>订单管理</h2></div>

    <el-card shadow="never" class="mb8">
      <el-form :inline="true" size="small">
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="订单号" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:110px">
            <el-option label="待付款" value="0" />
            <el-option label="待发货" value="1" />
            <el-option label="配送中" value="2" />
            <el-option label="已完成" value="3" />
            <el-option label="已取消" value="4" />
            <el-option label="售后中" value="5" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="10" class="mb8">
      <right-toolbar :search="false" @queryTable="fetchData" />
    </el-row>
    <el-row :gutter="10" class="mb8" v-if="selection.length && selection.some(r=>r.status==='1')">
      <el-col :span="1.5"><el-button type="warning" plain @click="batchDeliver">批量发货</el-button></el-col>
    </el-row>

    <el-table :data="list" v-loading="loading" border stripe @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column prop="orderNo" label="订单号" width="200" />
      <el-table-column prop="totalAmount" label="金额" width="100" align="right">
        <template #default="{ row }">¥{{ row.totalAmount }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="receiverName" label="收货人" width="100" />
      <el-table-column prop="receiverPhone" label="电话" width="120" />
      <el-table-column label="收货地址" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ (row.receiverProvince||'')+(row.receiverCity||'')+(row.receiverDistrict||'')+(row.receiverAddress||'') }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="160" />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
          <el-button v-if="row.status==='1'" link type="success" size="small" @click="showDeliver(row)">发货</el-button>
          <el-button v-if="row.status==='5'" link type="warning" size="small" @click="showDetail(row)">售后处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="fetchData" />

    <el-dialog v-model="deliverVisible" title="发货" width="400px">
      <el-form :model="deliverForm" label-width="90px">
        <el-form-item label="配送员姓名"><el-input v-model="deliverForm.deliveryName" /></el-form-item>
        <el-form-item label="配送员电话"><el-input v-model="deliverForm.deliveryPhone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deliverVisible=false">取消</el-button>
        <el-button type="primary" :loading="delivering" @click="handleDeliver">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MerchantOrder">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listOrder, deliverOrder } from '@/api/merchant/order'
import RightToolbar from '@/components/RightToolbar'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const total = ref(0)
const selection = ref([])
const delivering = ref(false)
const deliverVisible = ref(false)
const deliverForm = ref({ orderNo: '', deliveryName: '', deliveryPhone: '' })
const query = reactive({ orderNo: '', status: '', pageNum: 1, pageSize: 10 })

function statusType(s) { return { '0':'danger','1':'warning','2':'primary','3':'success','4':'info','5':'danger' }[s]||'info' }
function statusLabel(s) { return { '0':'待支付','1':'待发货','2':'配送中','3':'已完成','4':'已取消','5':'售后中' }[s]||s }

function resetQuery() { query.orderNo=''; query.status=''; query.pageNum=1; fetchData() }
function handleSelectionChange(s) { selection.value = s }

function fetchData() {
  loading.value = true
  const params = {}
  Object.keys(query).forEach(k => { if (query[k] !== '' && query[k] != null) params[k] = query[k] })
  listOrder(params).then(res => { list.value = res.data?.records || res.data || []; total.value = res.data?.total || list.value.length }).finally(() => { loading.value = false })
}

function batchDeliver() {
  const items = selection.value.filter(r => r.status==='1').map(r => ({ orderNo: r.orderNo, deliveryName: '', deliveryPhone: '' }))
  deliverOrder(items, true).then(() => { ElMessage.success('已提交批量发货'); fetchData() }).catch(() => ElMessage.warning('批量发货失败'))
}

function showDetail(row) { router.push('/merchant/order/detail/' + row.id) }
function showDeliver(row) { deliverForm.value = { orderNo: row.orderNo, deliveryName: '', deliveryPhone: '' }; deliverVisible.value = true }

function handleDeliver() {
  if (!deliverForm.value.deliveryName||!deliverForm.value.deliveryPhone) { ElMessage.warning('请填写配送员信息'); return }
  delivering.value = true
  deliverOrder(deliverForm.value).then(() => { ElMessage.success('发货成功'); deliverVisible.value = false; fetchData() }).finally(() => { delivering.value = false })
}

onMounted(fetchData)
</script>
