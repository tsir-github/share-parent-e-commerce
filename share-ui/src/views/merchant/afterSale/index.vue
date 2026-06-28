<template>
  <div class="merchant-after-sale">
    <div class="page-header"><h2>售后管理</h2></div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="orderNo" label="关联订单" width="220" />
      <el-table-column prop="refundAmount" label="退款金额" width="120" align="right">
        <template #default="{ row }">¥{{ row.refundAmount }}</template>
      </el-table-column>
      <el-table-column prop="refundReason" label="退款原因" min-width="160" show-overflow-tooltip />
      <el-table-column label="审核状态" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="auditType(row.auditStatus)">{{ auditLabel(row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="170" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <template v-if="row.auditStatus === '0'">
            <el-button link type="success" size="small" @click="handleApprove(row)">同意退款</el-button>
            <el-button link type="danger" size="small" @click="showReject(row)">拒绝</el-button>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="rejectVisible" title="拒绝退款" width="400px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入拒绝原因（选填）" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="handleReject">确认拒绝（转客服）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listAfterSale, approveAfterSale, rejectAfterSale } from '@/api/merchant/afterSale'

const list = ref([])
const loading = ref(false)
const rejectVisible = ref(false)
const rejecting = ref(false)
const rejectTarget = ref(null)
const rejectReason = ref('')

function auditType(s) { return { '0': 'warning', '1': 'success', '2': 'danger', '3': 'info' }[s] || 'info' }
function auditLabel(s) { return { '0': '待审核', '1': '已同意', '2': '已拒绝', '3': '客服介入' }[s] || s }

function fetchData() {
  loading.value = true
  listAfterSale().then(res => { list.value = res.data || [] }).finally(() => { loading.value = false })
}

function handleApprove(row) {
  approveAfterSale(row.id).then(() => {
    ElMessage.success('已同意退款')
    fetchData()
  })
}

function showReject(row) {
  rejectTarget.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

function handleReject() {
  rejecting.value = true
  rejectAfterSale(rejectTarget.value.id, rejectReason.value || '商家拒绝退款')
    .then(() => {
      ElMessage.success('已拒绝，转客服处理')
      rejectVisible.value = false
      fetchData()
    }).finally(() => { rejecting.value = false })
}

onMounted(fetchData)
</script>
