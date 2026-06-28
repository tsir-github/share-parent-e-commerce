<template>
  <div class="merchant-product-edit">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑商品' : '新增商品' }}</h2>
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="商品名称" prop="name">
        <el-input v-model="form.name" maxlength="100" />
      </el-form-item>
      <el-form-item label="副标题">
        <el-input v-model="form.subtitle" maxlength="200" />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-tree-select v-model="form.categoryId" :data="categoryTree" :props="{ label: 'name', value: 'id' }" placeholder="请选择" check-strictly filterable />
      </el-form-item>
      <el-form-item label="主图">
        <el-upload :auto-upload="false" :on-change="handleImageChange" :limit="1" list-type="picture-card">
          <el-icon><Plus /></el-icon>
        </el-upload>
      </el-form-item>
      <el-form-item label="单位">
        <el-input v-model="form.unit" placeholder="件/个/份" style="width:120px" />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="最低售价" prop="minPrice">
            <el-input-number v-model="form.minPrice" :precision="2" :min="0" style="width:180px" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="最高售价" prop="maxPrice">
            <el-input-number v-model="form.maxPrice" :precision="2" :min="0" style="width:180px" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="总库存">
            <el-input-number v-model="form.totalStock" :min="0" style="width:180px" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="商品描述">
        <el-input v-model="form.description" type="textarea" :rows="4" />
      </el-form-item>
      <el-divider>SKU 管理</el-divider>
      <SkuManager :product-id="editingId" ref="skuManagerRef" />
      <el-form-item>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
        <el-button @click="goBack">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getProduct, addProduct, updateProduct } from '@/api/merchant/product'
import SkuManager from './components/SkuManager.vue'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const skuManagerRef = ref()
const editingId = computed(() => route.params.id ? Number(route.params.id) : null)
const isEdit = computed(() => !!editingId.value)
const saving = ref(false)
const categoryTree = ref([])
const form = ref({ name: '', subtitle: '', categoryId: null, unit: '', minPrice: 0, maxPrice: 0, totalStock: 0, description: '', mainImage: '' })
const rules = { name: [{ required: true, message: '请输入商品名称' }], categoryId: [{ required: true, message: '请选择分类' }] }

function handleImageChange(uploadFile) {
  // ponytail: 简单 base64 预览，生产用 OSS
  const reader = new FileReader()
  reader.onload = e => { form.value.mainImage = e.target.result }
  reader.readAsDataURL(uploadFile.raw)
}

function goBack() { router.back() }

function handleSave() {
  formRef.value.validate(valid => {
    if (!valid) return
    saving.value = true
    const save = isEdit.value ? updateProduct(editingId.value, form.value) : addProduct(form.value)
    save.then(() => {
      ElMessage.success('保存成功')
      router.push('/merchant/product')
    }).finally(() => { saving.value = false })
  })
}

onMounted(() => {
  import('@/api/goods/category').then(m => m.treeselect()).then(res => { categoryTree.value = res.data || [] })
  if (editingId.value) {
    getProduct(editingId.value).then(res => {
      if (res.data) {
        const { id, createTime, updateTime, delFlag, merchantId, ...rest } = res.data
        form.value = { ...form.value, ...rest }
      }
    })
  }
})
</script>
