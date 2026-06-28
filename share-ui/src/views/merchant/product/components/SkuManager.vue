<template>
  <div class="sku-manager">
    <el-button size="small" type="primary" @click="addRow" style="margin-bottom:8px">添加规格</el-button>
    <el-table :data="skus" border stripe size="small">
      <el-table-column label="规格名称" min-width="160">
        <template #default="{ row, $index }">
          <el-input v-model="row.specs" placeholder="如: 红色/XL" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="售价" width="140">
        <template #default="{ row }">
          <el-input-number v-model="row.price" :precision="2" :min="0" size="small" style="width:130px" />
        </template>
      </el-table-column>
      <el-table-column label="库存" width="100">
        <template #default="{ row }">
          <el-input-number v-model="row.stock" :min="0" size="small" style="width:90px" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ $index }">
          <el-button link type="danger" size="small" @click="removeRow($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listSkuByProduct, addSku, updateSku, delSku } from '@/api/merchant/sku'

const props = defineProps({ productId: { type: Number, default: null } })
const skus = ref([])

function addRow() {
  skus.value.push({ specs: '', price: 0, stock: 0 })
}

function removeRow(index) {
  const removed = skus.value[index]
  if (removed.id) {
    delSku(removed.id).catch(() => ElMessage.error('删除SKU失败'))
  }
  skus.value.splice(index, 1)
}

watch(() => props.productId, (val) => {
  if (val) {
    listSkuByProduct(val).then(res => { skus.value = res.data || [] })
  }
}, { immediate: true })
</script>
