<template>
  <div class="app-container">
    <div class="page-header"><h2>商品管理</h2></div>

    <el-card shadow="never" class="mb8">
      <el-form :inline="true" :model="queryParams" size="small">
        <el-form-item label="名称"><el-input v-model="queryParams.name" placeholder="商品名称" clearable @keyup.enter="fetchData" style="width:160px" /></el-form-item>
        <el-form-item label="分类">
          <el-tree-select v-model="queryParams.categoryId" :data="categoryTree" :props="{label:'name',value:'id'}" placeholder="全部分类" clearable check-strictly filterable style="width:160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width:100px">
            <el-option label="上架" value="0" /><el-option label="下架" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="fetchData">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button></el-col>
      <el-col :span="1.5" v-if="selection.length">
        <el-button plain icon="Top" @click="batchStatus('0')">批量上架</el-button>
        <el-button plain icon="Bottom" type="warning" @click="batchStatus('1')">批量下架</el-button>
        <el-button plain icon="Delete" type="danger" @click="batchDelete">批量删除({{selection.length}})</el-button>
      </el-col>
      <right-toolbar :search="false" @queryTable="fetchData" />
    </el-row>

    <el-table :data="list" v-loading="loading" border stripe @selection-change="handleSelectionChange" @expand-change="handleExpand" style="width:100%" row-key="id">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div style="padding:10px 20px">
            <div v-if="!row._skusLoaded" style="text-align:center;color:#909399;padding:20px">加载中...</div>
            <div v-else-if="row._skus?.length">
              <el-table :data="row._skus" border size="small">
                <el-table-column label="图片" width="60">
                  <template #default="{row:r}"><el-image v-if="r.image" :src="r.image" style="width:32px;height:32px" fit="cover" /><span v-else style="color:#c0c4cc">-</span></template>
                </el-table-column>
                <el-table-column prop="specs" label="规格组合" min-width="130" />
                <el-table-column label="售价" width="90" align="right"><template #default="{row:r}">¥{{ r.price }}</template></el-table-column>
                <el-table-column label="原价" width="90" align="right"><template #default="{row:r}">¥{{ r.originalPrice || '-' }}</template></el-table-column>
                <el-table-column prop="stock" label="库存" width="70" align="center" />
                <el-table-column prop="sales" label="销量" width="70" align="center">
                  <template #default="{row:r}">{{ r.sales||0 }}</template>
                </el-table-column>
                <el-table-column label="状态" width="70" align="center">
                  <template #default="{row:r}"><el-tag :type="r.status==='0'?'success':'info'" size="small">{{ r.status==='0'?'启用':'禁用' }}</el-tag></template>
                </el-table-column>
              </el-table>
            </div>
            <div v-else style="text-align:center;color:#909399;padding:20px">暂无SKU</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column type="selection" width="45" align="center" />
      <el-table-column label="图片" width="70">
        <template #default="{ row }"><el-image v-if="row.mainImage" :src="row.mainImage" style="width:44px;height:44px" fit="cover" /><span v-else style="color:#c0c4cc">-</span></template>
      </el-table-column>
      <el-table-column prop="name" label="商品名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="subtitle" label="副标题" min-width="120" show-overflow-tooltip />
      <el-table-column label="分类" width="110" align="center">
        <template #default="{ row }">{{ getCategoryName(row.categoryId) || row.categoryId || '-' }}</template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="55" align="center" />
      <el-table-column prop="sales" label="总销量" width="65" align="center" />
      <el-table-column prop="totalStock" label="SKU总库存" width="80" align="center">
        <template #header><span>SKU总库存 <el-tooltip content="所有SKU库存之和（product_sku.stock汇总）" placement="top"><el-icon style="color:#909399;font-size:12px"><QuestionFilled /></el-icon></el-tooltip></span></template>
      </el-table-column>
      <el-table-column label="SKU最低售价" width="100" align="right"><template #default="{ row }">¥{{ row.minPrice || '-' }}</template>
        <template #header><span>SKU最低价 <el-tooltip content="所有SKU中最便宜的价格" placement="top"><el-icon style="color:#909399;font-size:12px"><QuestionFilled /></el-icon></el-tooltip></span></template>
      </el-table-column>
      <el-table-column label="SKU最高售价" width="100" align="right"><template #default="{ row }">¥{{ row.maxPrice || '-' }}</template>
        <template #header><span>SKU最高价 <el-tooltip content="所有SKU中最贵的价格" placement="top"><el-icon style="color:#909399;font-size:12px"><QuestionFilled /></el-icon></el-tooltip></span></template>
      </el-table-column>
      <el-table-column label="标签" width="150" align="center">
        <template #default="{ row }">
          <div style="display:flex;gap:2px;flex-wrap:wrap;justify-content:center">
          <el-tag v-if="row.isNew==='1'" type="danger" size="small" effect="plain" style="margin:0 2px">新品</el-tag>
          <el-tag v-if="row.isHot==='1'" type="warning" size="small" effect="plain" style="margin:0 2px">热销</el-tag>
          <el-tag v-if="row.isRecommended==='1'" type="success" size="small" effect="plain" style="margin:0 2px">推荐</el-tag>
          <span v-if="row.isNew!=='1'&&row.isHot!=='1'&&row.isRecommended!=='1'" style="color:#c0c4cc">-</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="75" align="center">
        <template #default="{ row }"><el-tag :type="row.status==='0'?'success':'info'" size="small">{{ row.status==='0'?'上架':'下架' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="创建时间" width="150" align="center"><template #default="{ row }">{{ row.createTime?.substring(0,16) || '-' }}</template></el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-tooltip content="编辑"><el-button link type="primary" icon="Edit" @click="handleEdit(row)" /></el-tooltip>
          <el-tooltip content="管理SKU"><el-button link type="primary" icon="Setting" @click="showSkuDialog(row)" /></el-tooltip>
          <el-tooltip :content="row.status==='0'?'下架':'上架'"><el-button link :type="row.status==='0'?'warning':''" :icon="row.status==='0'?'Bottom':'Top'" @click="toggleStatus(row)" /></el-tooltip>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row)"><template #reference><el-tooltip content="删除"><el-button link type="danger" icon="Delete" /></el-tooltip></template></el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="fetchData" />

    <!-- SKU 管理弹窗 -->
    <el-dialog v-model="skuDialogOpen" :title="'SKU管理 - ' + skuProductName" width="900px" append-to-body @close="skuDialogOpen=false">
      <SkuManager :product-id="skuProductId" ref="skuDialogRef" />
      <template #footer>
        <el-button @click="skuDialogOpen=false">关闭</el-button>
        <el-button type="primary" @click="saveSkuDialog">保存SKU</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MerchantProduct">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listProduct, delProduct, updateProduct } from '@/api/merchant/product'
import { listSkuByProduct } from '@/api/merchant/sku'
import SkuManager from './components/SkuManager.vue'
import RightToolbar from '@/components/RightToolbar'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const total = ref(0)
const selection = ref([])
const categoryTree = ref([])
const queryParams = ref({ name:'', status:'', categoryId:'', pageNum:1, pageSize:10 })

function resetQuery() { queryParams.value = { name:'', status:'', categoryId:'', pageNum:1, pageSize:10 }; fetchData() }
function handleSelectionChange(s) { selection.value = s }

function fetchData() {
  loading.value = true
  listProduct(queryParams.value).then(res => {
    let data = res.data?.records || res.data || []
    // 前端筛选
    if (queryParams.value.name) data = data.filter(r => r.name?.includes(queryParams.value.name))
    if (queryParams.value.status) data = data.filter(r => r.status === queryParams.value.status)
    if (queryParams.value.categoryId) data = data.filter(r => r.categoryId === queryParams.value.categoryId)
    total.value = data.length
    list.value = data.slice((queryParams.value.pageNum-1)*queryParams.value.pageSize, queryParams.value.pageNum*queryParams.value.pageSize)
    loading.value = false
  })
}

function batchStatus(status) {
  const ids = selection.value.map(r => r.id)
  const text = status === '0' ? '上架' : '下架'
  Promise.all(ids.map(id => updateProduct(id, { status }))).then(() => { ElMessage.success(`已批量${text}`); fetchData() })
}

function batchDelete() {
  ElMessageBox.confirm(`确认删除选中的 ${selection.value.length} 个商品？`, '批量删除', { type:'warning' }).then(() => {
    Promise.all(selection.value.map(r => delProduct(r.id))).then(() => { ElMessage.success('已批量删除'); fetchData() })
  })
}

function handleAdd() { router.push('/merchant/product/add') }
function handleEdit(row) { router.push('/merchant/product/edit/' + row.id) }

function toggleStatus(row) {
  const ns = row.status === '0' ? '1' : '0'
  updateProduct(row.id, { status: ns }).then(() => { ElMessage.success(ns==='0'?'已上架':'已下架'); fetchData() })
}

function handleDelete(row) {
  delProduct(row.id).then(() => { ElMessage.success('已删除'); fetchData() }).catch(() => ElMessage.error('删除失败'))
}

// 展开行 → 懒加载SKU
function handleExpand(row, expandedRows) {
  const isExpanding = expandedRows.some(r => r.id === row.id)
  if (isExpanding && !row._skusLoaded) {
    listSkuByProduct(row.id).then(res => {
      row._skus = res.data || []
      row._skusLoaded = true
    })
  }
}

// SKU弹窗
const skuDialogOpen = ref(false)
const skuProductId = ref(null)
const skuProductName = ref('')
const skuDialogRef = ref()
function showSkuDialog(row) {
  skuProductId.value = row.id
  skuProductName.value = row.name
  skuDialogOpen.value = true
  row._skusLoaded = false
}
async function saveSkuDialog() {
  if (skuDialogRef.value) {
    await skuDialogRef.value.saveAll(skuProductId.value)
    ElMessage.success('SKU已保存'); skuDialogOpen.value = false
    const item = list.value.find(r => r.id === skuProductId.value)
    if (item) { item._skusLoaded = false; item._skus = null }
    fetchData()
  }
}

const categoryMap = ref({})
function getCategoryName(id) { return categoryMap.value[id] || '' }

onMounted(() => {
  import('@/api/goods/category').then(m => m.treeselect()).then(res => {
    const tree = res.data || []; categoryTree.value = tree
    // 构建分类ID→名称映射
    function walk(nodes) { nodes.forEach(n => { categoryMap.value[n.id]=n.name; if(n.children) walk(n.children) }) }
    walk(tree)
  }).catch(() => {})
  fetchData()
})
</script>
