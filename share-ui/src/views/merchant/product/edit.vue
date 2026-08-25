<template>
  <div class="app-container">
    <div class="page-header"><h2>{{ isEdit ? '编辑商品' : '新增商品' }}</h2></div>

    <el-card shadow="never" class="mb8">
      <template #header><span>基本信息</span></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="30">
          <el-col :span="14">
            <el-form-item label="商品名称" prop="name"><el-input v-model="form.name" maxlength="100" /></el-form-item>
            <el-form-item label="副标题"><el-input v-model="form.subtitle" maxlength="200" /></el-form-item>
            <el-form-item label="分类" prop="categoryId">
              <el-tree-select v-model="form.categoryId" :data="categoryTree" :props="{label:'name',value:'id'}" placeholder="请选择分类" check-strictly filterable style="width:100%" />
            </el-form-item>
            <el-form-item label="商品描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
            <el-form-item label="单位"><el-input v-model="form.unit" placeholder="件/个/份" style="width:120px" /></el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="主图">
              <el-upload :action="uploadUrl" :headers="uploadHeaders" :on-success="onMainImageSuccess" :before-upload="beforeImageUpload" :show-file-list="false" accept="image/*" name="file">
                <img v-if="form.mainImage" :src="form.mainImage" class="main-image-preview" />
                <el-icon v-else class="uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
            <el-divider>营销标签</el-divider>
            <el-form-item label="标记新品"><el-switch v-model="form.isNew" active-value="1" inactive-value="0" /></el-form-item>
            <el-form-item label="标记热销"><el-switch v-model="form.isHot" active-value="1" inactive-value="0" /></el-form-item>
            <el-form-item label="标记推荐"><el-switch v-model="form.isRecommended" active-value="1" inactive-value="0" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 商品参数（动态属性键值对 → ext_json） -->
    <el-card shadow="never" class="mb8">
      <template #header>
        <span>商品参数 <span style="font-size:12px;color:#999;font-weight:400">（产地、保质期等，商家自定义）</span></span>
        <el-button size="small" type="primary" style="float:right" @click="addAttr">添加属性</el-button>
      </template>
      <el-row :gutter="10" v-for="(attr, i) in attrs" :key="i" style="margin-bottom:8px">
        <el-col :span="10"><el-input v-model="attr.key" placeholder="属性名（如：产地）" /></el-col>
        <el-col :span="1" style="text-align:center;line-height:32px">:</el-col>
        <el-col :span="10"><el-input v-model="attr.value" placeholder="属性值（如：河南郑州）" /></el-col>
        <el-col :span="3"><el-button type="danger" plain icon="Delete" circle size="small" @click="removeAttr(i)" :disabled="attrs.length===1" /></el-col>
      </el-row>
    </el-card>

    <el-card shadow="never" class="mb8">
      <template #header>
        <span>SKU 管理 (价格库存由此决定)</span>
        <span v-if="skuTotals.totalStock" style="float:right;color:#909399;font-size:13px">
          最低 ¥{{ skuTotals.minPrice }} / 最高 ¥{{ skuTotals.maxPrice }} / 总库存 {{ skuTotals.totalStock }}
        </span>
      </template>
      <SkuManager :product-id="editingId" ref="skuManagerRef" />
    </el-card>

    <el-row>
      <el-col :span="24" style="text-align:center">
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
        <el-button @click="goBack">取消</el-button>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getProduct, addProduct, updateProduct } from '@/api/merchant/product'
import { getToken } from '@/utils/auth'
import SkuManager from './components/SkuManager.vue'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const skuManagerRef = ref()
const editingId = computed(() => route.params.id || null)  // Snowflake ID 超出 JS Number 精度，保持字符串
const isEdit = computed(() => !!editingId.value)
const saving = ref(false)
const categoryTree = ref([])
const skuTotals = reactive({ minPrice: 0, maxPrice: 0, totalStock: 0 })

// 商品参数（动态属性键值对）
const attrs = ref([{ key: '', value: '' }])
function addAttr() { attrs.value.push({ key: '', value: '' }) }
function removeAttr(i) { if (attrs.value.length > 1) attrs.value.splice(i, 1) }
function buildExtJson() {
  const obj = {}
  attrs.value.forEach(a => { if (a.key.trim()) obj[a.key.trim()] = a.value.trim() })
  return Object.keys(obj).length ? JSON.stringify(obj) : ''
}
function parseExtJson(jsonStr) {
  if (!jsonStr) return
  try {
    const obj = JSON.parse(jsonStr)
    const arr = Object.entries(obj).map(([k, v]) => ({ key: k, value: v }))
    if (arr.length) attrs.value = arr
  } catch(e) {}
}

const form = ref({ name:'',subtitle:'',categoryId:null,unit:'',minPrice:0,maxPrice:0,totalStock:0,description:'',mainImage:'',isNew:'0',isHot:'0',isRecommended:'0',extJson:'' })
const rules = { name:[{required:true,message:'请输入商品名称'}], categoryId:[{required:true,message:'请选择分类'}] }

const uploadUrl = computed(() => '/file-upload/upload')
const uploadHeaders = computed(() => ({ Authorization: 'Bearer ' + getToken() }))
function beforeImageUpload(file) {
  if (!file.type.startsWith('image/')) { ElMessage.error('只能上传图片'); return false }
  if (file.size > 10*1024*1024) { ElMessage.error('图片不能超过10MB'); return false }
  return true
}
function onMainImageSuccess(res) {
  if (res.data?.url) form.value.mainImage = res.data.url
}

function goBack() { router.back() }

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  
  // 汇总SKU数据到产品
  const totals = skuManagerRef.value?.getTotals()
  if (totals) {
    form.value.minPrice = totals.minPrice
    form.value.maxPrice = totals.maxPrice
    form.value.totalStock = totals.totalStock
  }
  form.value.extJson = buildExtJson()

  const save = isEdit.value
    ? updateProduct(editingId.value, form.value)
    : addProduct(form.value)

  save.then(async (res) => {
    ElMessage.success('保存成功')
    // 保存SKU
    const pid = isEdit.value ? editingId.value : res.data
    if (pid && skuManagerRef.value) {
      await skuManagerRef.value.saveAll(pid)
    }
    router.push('/merchant/product')
  }).finally(() => { saving.value = false })
}

onMounted(() => {
  import('@/api/goods/category').then(m => m.treeselect()).then(res => { categoryTree.value = res.data||[] })
  if (editingId.value) {
    getProduct(editingId.value).then(res => {
      if (res.data) {
        const { id, createTime, updateTime, delFlag, merchantId, ...rest } = res.data
        form.value = { ...form.value, ...rest }
        // 回显商品参数
        if (res.data.extJson) parseExtJson(res.data.extJson)
      }
    })
  }
})
</script>

<style scoped>
.main-image-preview { width: 100px; height: 100px; border-radius: 6px; object-fit: cover; border: 1px solid #dcdfe6; }
.uploader-icon { font-size: 28px; color: #c0c4cc; width: 100px; height: 100px; line-height: 100px; text-align: center; border: 1px dashed #dcdfe6; border-radius: 6px; cursor: pointer; }
.uploader-icon:hover { border-color: #409eff; color: #409eff; }
</style>
