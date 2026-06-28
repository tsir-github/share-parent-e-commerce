<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="queryParams.nickname" placeholder="请输入昵称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="queryParams.phone" placeholder="请输入手机号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="用户状态" clearable style="width: 200px">
          <el-option label="正常" value="1" />
          <el-option label="禁用" value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['user:userInfo:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['user:userInfo:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['user:userInfo:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="userInfoList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="昵称" align="center" prop="nickname" :show-overflow-tooltip="true" />
      <el-table-column label="手机号" align="center" prop="phone" width="130" />
      <el-table-column label="性别" align="center" prop="gender" width="60">
        <template #default="scope">
          <span>{{ scope.row.gender === '1' ? '男' : scope.row.gender === '2' ? '女' : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80">
        <template #default="scope">
          <el-switch v-model="scope.row.status" active-value="1" inactive-value="2" @change="handleStatusChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="最后登录IP" align="center" prop="lastLoginIp" width="140" />
      <el-table-column label="最后登录时间" align="center" prop="lastLoginTime" width="170">
        <template #default="scope">
          <span>{{ parseTime(scope.row.lastLoginTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['user:userInfo:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['user:userInfo:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form :model="form" :rules="rules" ref="userInfoRef" label-width="80px">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="50" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" placeholder="请选择">
            <el-option label="男" value="1" />
            <el-option label="女" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="1">正常</el-radio>
            <el-radio value="2">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="UserInfo">
import { getCurrentInstance } from 'vue'
import { listUserInfo, getUserInfo, addUserInfo, updateUserInfo, delUserInfo } from '@/api/user/userInfo'

const { proxy } = getCurrentInstance()

const userInfoList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    nickname: undefined,
    phone: undefined,
    status: undefined
  },
  rules: {}
})

const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listUserInfo(queryParams.value).then(res => {
    userInfoList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleStatusChange(row) {
  const text = row.status === '1' ? '启用' : '停用'
  proxy.$modal.confirm('确认要"' + text + '""' + row.nickname + '"用户吗?').then(() => {
    return updateUserInfo(row)
  }).then(() => {
    proxy.$modal.msgSuccess(text + '成功')
  }).catch(() => {
    row.status = row.status === '1' ? '2' : '1'
  })
}

function reset() {
  form.value = {
    id: undefined,
    nickname: undefined,
    phone: undefined,
    gender: undefined,
    status: '1'
  }
  proxy.resetForm('userInfoRef')
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '添加用户'
}

function handleUpdate(row) {
  reset()
  const id = row.id || ids.value
  getUserInfo(id).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改用户'
  })
}

function handleDelete(row) {
  const idsToDel = row.id || ids.value
  proxy.$modal.confirm('是否确认删除该用户？').then(() => {
    return delUserInfo(idsToDel)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function submitForm() {
  proxy.$refs['userInfoRef'].validate(valid => {
    if (valid) {
      if (form.value.id !== undefined) {
        updateUserInfo(form.value).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addUserInfo(form.value).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

function cancel() {
  open.value = false
  reset()
}

getList()
</script>
