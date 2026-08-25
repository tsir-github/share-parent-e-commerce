<template>
  <div class="sku-manager">
    <el-button size="small" type="primary" @click="addRow" style="margin-bottom:8px">添加规格</el-button>
    <el-table :data="skus" border stripe size="small">
      <el-table-column label="规格组合" min-width="140">
        <template #header><span>规格组合 <el-tooltip content="格式: 属性/值，多属性用空格分隔。例子: '红色/XL'、'500ml/瓶装'、'经典味/袋装'" placement="top"><el-icon style="color:#909399"><QuestionFilled /></el-icon></el-tooltip></span></template>
        <template #default="{ row }">
          <el-input v-model="row.specs" placeholder="红色/XL 或 500ml/瓶装" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="售价(¥)" width="120">
        <template #default="{ row }">
          <el-input-number v-model="row.price" :precision="2" :min="0" size="small" style="width:110px" />
        </template>
      </el-table-column>
      <el-table-column label="原价(¥)" width="120">
        <template #default="{ row }">
          <el-input-number v-model="row.originalPrice" :precision="2" :min="0" size="small" style="width:110px" placeholder="划线价" />
        </template>
      </el-table-column>
      <el-table-column label="库存" width="80">
        <template #default="{ row }">
          <el-input-number v-model="row.stock" :min="0" size="small" style="width:70px" />
        </template>
      </el-table-column>
      <el-table-column label="图片" width="80">
        <template #default="{ row }">
          <el-upload :action="uploadUrl" :headers="uploadHeaders" :on-success="(res) => onSkuImage(res, row)" :before-upload="beforeImageUpload" :show-file-list="false" accept="image/*" name="file">
            <img v-if="row.image" :src="row.image" class="sku-image" />
            <el-icon v-else class="sku-upload-icon"><Plus /></el-icon>
          </el-upload>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row, $index }">
          <el-button link type="primary" size="small" @click="copyRow($index)">复制</el-button>
          <el-button link type="danger" size="small" @click="removeRow($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listSkuByProduct, addSku, updateSku, delSku } from '@/api/merchant/sku'
import { getToken } from '@/utils/auth'

const props = defineProps({ productId: { type: [Number, String], default: null } })
const skus = ref([])

const uploadUrl = computed(() => '/file-upload/upload')
const uploadHeaders = computed(() => ({ Authorization: 'Bearer ' + getToken() }))
function beforeImageUpload(file) { if (!file.type.startsWith('image/')) { ElMessage.error('只能上传图片'); return false }; return true }
function onSkuImage(res, row) { if (res.data?.url) row.image = res.data.url }

function addRow() { skus.value.push({ specs: '', price: 0, originalPrice: 0, stock: 0, image: '' }) }

function removeRow(index) {
  const removed = skus.value[index]
  if (removed.id) delSku(removed.id).catch(() => ElMessage.error('删除SKU失败'))
  skus.value.splice(index, 1)
}

function copyRow(index) {
  skus.value.push({ ...skus.value[index], id: null, specs: '' })
}

async function saveAll(productId) {
  if (!productId || skus.value.length === 0) return
  const promises = skus.value
    .filter(s => s.specs.trim())
    .map(s => {
      const data = { specs: s.specs, price: s.price, originalPrice: s.originalPrice, stock: s.stock, image: s.image }
      return s.id ? updateSku(s.id, data) : addSku({ productId, ...data })
    })
  await Promise.all(promises)
}

function getTotals() {
  const valid = skus.value.filter(s => s.specs.trim())
  const prices = valid.map(s => s.price).filter(p => p > 0)
  const stocks = valid.map(s => s.stock).filter(s => s >= 0)
  return { minPrice: prices.length ? Math.min(...prices) : 0, maxPrice: prices.length ? Math.max(...prices) : 0, totalStock: stocks.reduce((a, b) => a + b, 0) }
}

watch(() => props.productId, (val) => {
  if (val) listSkuByProduct(val).then(res => { skus.value = res.data || [] })
}, { immediate: true })

defineExpose({ saveAll, getTotals })
</script>

<style scoped>
.sku-image { width: 32px; height: 32px; border-radius: 3px; object-fit: cover; border: 1px solid #dcdfe6; cursor: pointer; }
.sku-upload-icon { font-size: 18px; color: #c0c4cc; width: 32px; height: 32px; line-height: 32px; text-align: center; border: 1px dashed #dcdfe6; border-radius: 3px; cursor: pointer; }
.sku-upload-icon:hover { border-color: #409eff; color: #409eff; }
</style>
