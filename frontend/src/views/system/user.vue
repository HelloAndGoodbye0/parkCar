<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span class="card-title">用户管理</span>
        <el-button type="primary" @click="openDialog()">新增用户</el-button>
      </div>
    </template>

    <div style="margin-bottom: 12px; display: flex; gap: 8px">
      <el-input v-model="query.keyword" placeholder="用户名 / 姓名" clearable style="width: 220px" @keyup.enter="load" />
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px" @change="load">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="records" v-loading="loading">
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column label="角色" width="160">
        <template #default="{ row }">
          <el-tag
            v-for="role in row.roles"
            :key="role"
            :type="role === 'ADMIN' ? 'danger' : 'success'"
            size="small"
            style="margin-right: 4px"
          >
            {{ role === 'ADMIN' ? '管理员' : '收费员' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="负责区域" width="200">
        <template #default="{ row }">
          <template v-if="row.areaIds && row.areaIds.length">
            <el-tag v-for="aid in row.areaIds" :key="aid" type="info" size="small" style="margin-right: 4px">
              {{ areaName(aid) }}
            </el-tag>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <div class="op-group">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="warning" @click="onResetPassword(row)">重置密码</el-button>
            <el-button
              v-if="row.username !== userStore.userInfo?.username"
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="onToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button
              v-if="row.username !== userStore.userInfo?.username"
              link
              type="danger"
              @click="onDelete(row)"
            >
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
      :page-size="query.size"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 12px; justify-content: flex-end"
      @current-change="load"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="密码" v-if="!form.id" prop="password">
          <el-input v-model="form.password" placeholder="默认 123456" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="角色">
          <el-checkbox-group v-model="form.roleIds">
            <el-checkbox v-for="role in roles" :key="role.id" :value="role.id">
              {{ role.name }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="负责区域">
          <el-select v-model="form.areaIds" multiple clearable placeholder="请选择收费员负责的区域" style="width: 100%">
            <el-option v-for="a in areas" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
          <div style="color: #909399; font-size: 12px; line-height: 1.6; width: 100%">
            仅对收费员生效（按区域隔离数据），管理员不受区域限制
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
defineOptions({ name: 'SystemUser' })
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getUsers, getRoles, getAreas, createUser, updateUser,
  changeUserStatus, resetUserPassword, deleteUser
} from '@/api'

const userStore = useUserStore()
const records = ref([])
const roles = ref([])
const areas = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 10, keyword: '', status: null })
const form = reactive({ id: null, username: '', password: '', realName: '', phone: '', roleIds: [], areaIds: [] })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const data = await getUsers(query)
    records.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const loadRoles = async () => {
  roles.value = await getRoles()
}

const loadAreas = async () => {
  areas.value = await getAreas()
}

const areaName = (id) => {
  const a = areas.value.find((x) => x.id === id)
  return a ? a.name : id
}

const openDialog = (row) => {
  if (row) {
    Object.assign(form, {
      id: row.id, username: row.username, password: '', realName: row.realName,
      phone: row.phone, roleIds: row.roles.includes('ADMIN')
        ? roles.value.filter((r) => r.code === 'ADMIN').map((r) => r.id)
        : roles.value.filter((r) => r.code === 'OPERATOR').map((r) => r.id),
      areaIds: row.areaIds || []
    })
  } else {
    Object.assign(form, { id: null, username: '', password: '', realName: '', phone: '', roleIds: [], areaIds: [] })
  }
  dialogVisible.value = true
}

const onSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateUser(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await createUser(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const onResetPassword = (row) => {
  ElMessageBox.prompt(`为 ${row.username} 设置新密码`, '重置密码', {
    inputPattern: /^.{6,}$/,
    inputErrorMessage: '密码至少 6 位'
  }).then(async ({ value }) => {
    await resetUserPassword(row.id, { password: value })
    ElMessage.success('密码已重置')
  }).catch(() => {})
}

const onToggleStatus = (row) => {
  const next = row.status === 1 ? 0 : 1
  ElMessageBox.confirm(`确定${next === 1 ? '启用' : '禁用'}用户 ${row.username} 吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await changeUserStatus(row.id, { status: next })
      ElMessage.success('操作成功')
      load()
    })
    .catch(() => {})
}

const onDelete = (row) => {
  ElMessageBox.confirm(`确定删除用户 ${row.username} 吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteUser(row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

onMounted(async () => {
  await loadRoles()
  await loadAreas()
  await load()
})
</script>

<style scoped>
.card-title {
  font-weight: 600;
}

/* 操作按钮组：flex 换行后各行动线对齐，并覆盖 Element Plus 按钮默认左边距 */
.op-group {
  display: flex;
  flex-wrap: wrap;
  gap: 0 12px;
}
.op-group :deep(.el-button + .el-button) {
  margin-left: 0;
}
</style>
