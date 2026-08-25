<template>
  <div class="app-container">
    <div class="page-header"><h2>售后管理</h2></div>

    <el-card shadow="never" class="mb8">
      <el-form :inline="true" size="small">
        <el-form-item label="审核状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable style="width:130px" @change="fetchData">
            <el-option label="待审核" value="0" /><el-option label="已同意" value="1" /><el-option label="已拒绝" value="2" /><el-option label="客服介入" value="3" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="10" class="mb8"><right-toolbar :search="false" @queryTable="fetchData" /></el-row>

    <el-table :data="list" v-loading="loading" border stripe @selection-change="s=>selection=s">
      <el-table-column type="selection" width="45" align="center" />
      <el-table-column prop="orderNo" label="关联订单" width="190" />
      <el-table-column prop="refundAmount" label="退款金额" width="110" align="right"><template #default="{row}">¥{{ row.refundAmount }}</template></el-table-column>
      <el-table-column prop="refundReason" label="退款原因" min-width="160" show-overflow-tooltip />
      <el-table-column label="审核状态" width="110" align="center">
        <template #default="{row}"><el-tag :type="auditType(row.auditStatus)" size="small">{{ auditLabel(row.auditStatus) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="160" />
      <el-table-column label="操作" width="240" align="center" fixed="right">
        <template #default="{row}">
          <el-button link type="primary" size="small" icon="View" @click="showDetail(row)">详情</el-button>
          <template v-if="row.auditStatus==='0'">
            <el-button link type="success" size="small" @click="handleApprove(row)">同意退款</el-button>
            <el-button link type="danger" size="small" @click="showReject(row)">拒绝</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="pageNum" v-model:limit="pageSize" @pagination="fetchData" />

    <el-dialog title="售后详情" v-model="detailOpen" width="550px" append-to-body>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="申请ID" :span="2">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="关联订单">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="退款金额"><span style="color:#f56c6c;font-weight:600">¥{{ detail.refundAmount }}</span></el-descriptions-item>
        <el-descriptions-item label="退款原因" :span="2">{{ detail.refundReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核状态"><el-tag :type="auditType(detail.auditStatus)" size="small">{{ auditLabel(detail.auditStatus) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="审核备注">{{ detail.auditRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核时间" :span="2">{{ detail.auditTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核人" :span="2">{{ detail.auditBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请时间" :span="2">{{ detail.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="拒绝退款" width="400px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入拒绝原因（选填）" />
      <template #footer>
        <el-button @click="rejectVisible=false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="handleReject">确认拒绝（转客服）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MerchantAfterSale">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listAfterSale, approveAfterSale, rejectAfterSale } from '@/api/merchant/afterSale'
import RightToolbar from '@/components/RightToolbar'

const list = ref([]); const loading = ref(false); const total = ref(0)
const pageNum = ref(1); const pageSize = ref(10); const statusFilter = ref(''); const selection = ref([])
const rejectVisible = ref(false); const rejecting = ref(false); const rejectTarget = ref(null); const rejectReason = ref('')
const detailOpen = ref(false); const detail = ref({})

function auditType(s) { return {'0':'warning','1':'success','2':'danger','3':'info'}[s]||'info' }
function auditLabel(s) { return {'0':'待审核','1':'已同意','2':'已拒绝','3':'客服介入'}[s]||s }

function fetchData() {
  loading.value=true
  listAfterSale().then(res => {
    let data = res.data||[]
    if (statusFilter.value) data = data.filter(r=>r.auditStatus===statusFilter.value)
    total.value=data.length
    list.value=data.slice((pageNum.value-1)*pageSize.value, pageNum.value*pageSize.value)
    loading.value=false
  })
}

function showDetail(row) { detail.value = row; detailOpen.value = true }
function handleApprove(row) { approveAfterSale(row.id).then(()=>{ ElMessage.success('已同意退款'); fetchData() }) }
function showReject(row) { rejectTarget.value=row; rejectReason.value=''; rejectVisible.value=true }
function handleReject() {
  rejecting.value=true
  rejectAfterSale(rejectTarget.value.id, rejectReason.value||'商家拒绝退款').then(()=>{ ElMessage.success('已拒绝，转客服处理'); rejectVisible.value=false; fetchData() }).finally(()=>{ rejecting.value=false })
}

onMounted(fetchData)
</script>
