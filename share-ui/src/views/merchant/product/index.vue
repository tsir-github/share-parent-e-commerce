<template>
  <div class="merchant-product">
    <div class="page-header">
      <h2>商品管理</h2>
    </div>
    <div class="search-bar">
      <el-form :inline="true" :model="queryParams" size="small">
        <el-form-item label="商品名称">
          <el-input v-model="queryParams.name" placeholder="请输入" clearable @keyup.enter="fetchData" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width:120px">
            <el-option label="上架" value="0" />
            <el-option label="下架" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">新增商品</el-button>
    </div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column label="主图" width="80">
        <template #default="{ row }">
          <el-image v-if="row.mainImage" :src="row.mainImage" style="width:50px;height:50px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="商品名称" min-width="200" />
      <el-table-column prop="minPrice" label="最低售价" width="120" align="right">
        <template #default="{ row }">¥{{ row.minPrice }}</template>
      </el-table-column>
      <el-table-column prop="totalStock" label="库存" width="80" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'success' : 'info'">{{ row.status === '0' ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link size="small" @click="toggleStatus(row)">{{ row.status === '0' ? '下架' : '上架' }}</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button link type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listProduct, delProduct, updateProduct } from '@/api/merchant/product'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const queryParams = ref({ name: '', status: '' })

function resetQuery() {
  queryParams.value = { name: '', status: '' }
  fetchData()
}

function fetchData() {
  loading.value = true
  const params = { ...queryParams.value }
  Object.keys(params).forEach(k => { if (!params[k]) delete params[k] })
  listProduct(params)
    .then(res => { list.value = res.data || [] })
    .finally(() => { loading.value = false })
}

function handleAdd() {
  router.push('/merchant/product/add')
}

function handleEdit(row) {
  router.push('/merchant/product/edit/' + row.id)
}

function toggleStatus(row) {
  const newStatus = row.status === '0' ? '1' : '0'
  updateProduct(row.id, { status: newStatus }).then(() => {
    ElMessage.success(newStatus === '0' ? '已上架' : '已下架')
    fetchData()
  })
}

function handleDelete(row) {
  delProduct(row.id).then(() => {
    ElMessage.success('已删除')
    fetchData()
  })
}

onMounted(fetchData)
</script>
