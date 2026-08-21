<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="5" v-for="s in stats" :key="s.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">{{ s.label }}</div>
          <div class="stat-value" :style="{ color: s.color }">{{ s.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
          <span class="card-title">车位列表</span>
          <div style="display: flex; gap: 8px; flex-wrap: wrap">
            <el-select v-model="query.areaId" placeholder="全部区域" clearable style="width: 130px" @change="load">
              <el-option v-for="a in areas" :key="a.id" :label="a.name" :value="a.id" />
            </el-select>
            <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 130px" @change="load">
              <el-option label="普通" :value="0" />
              <el-option label="充电" :value="1" />
              <el-option label="无障碍" :value="2" />
              <el-option label="VIP" :value="3" />
            </el-select>
            <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="load">
              <el-option label="空闲" :value="0" />
              <el-option label="占用" :value="1" />
              <el-option label="禁用" :value="2" />
              <el-option label="维护" :value="3" />
            </el-select>
            <el-button v-if="userStore.isAdmin" type="primary" @click="openCreate">新增车位</el-button>
            <el-button v-if="userStore.isAdmin" type="success" @click="openBatch">批量新增</el-button>
          </div>
        </div>
      </template>

      <el-table :data="records" v-loading="loading">
        <el-table-column prop="spaceNo" label="车位编号" width="110" />
        <el-table-column prop="areaName" label="区域" width="100" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ typeText(row.type) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="190" fixed="right" v-if="userStore.isAdmin">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-dropdown v-if="row.status !== 1" @command="(cmd) => onStatus(row, cmd)">
              <el-button link type="warning">状态调整</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="2" v-if="row.status !== 2">禁用</el-dropdown-item>
                  <el-dropdown-item command="3" v-if="row.status !== 3">维护</el-dropdown-item>
                  <el-dropdown-item command="0" v-if="row.status !== 0">恢复正常</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
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
    </el-card>

    <!-- 新增/编辑单个车位 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑车位' : '新增车位'" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="区域" prop="areaId">
          <el-select v-model="form.areaId" style="width: 100%">
            <el-option v-for="a in areas" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="车位编号" prop="spaceNo">
          <el-input v-model="form.spaceNo" placeholder="如：A-101" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="普通" :value="0" />
            <el-option label="充电" :value="1" />
            <el-option label="无障碍" :value="2" />
            <el-option label="VIP" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量新增 -->
    <el-dialog v-model="batchVisible" title="批量新增车位" width="520px">
      <el-form label-width="80px">
        <el-form-item label="区域">
          <el-select v-model="batchForm.areaId" style="width: 100%">
            <el-option v-for="a in areas" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="batchForm.type" style="width: 100%">
            <el-option label="普通" :value="0" />
            <el-option label="充电" :value="1" />
            <el-option label="无障碍" :value="2" />
            <el-option label="VIP" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="车位编号">
          <el-input
            v-model="batchForm.nos"
            type="textarea"
            :rows="5"
            placeholder="每行一个编号，如：&#10;D-001&#10;D-002&#10;D-003"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onBatchSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
defineOptions({ name: 'SpaceSpace' })
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getSpaces, getSpaceOverview, getAreas,
  createSpace, updateSpace, deleteSpace, changeSpaceStatus, batchCreateSpace
} from '@/api'

const userStore = useUserStore()
const records = ref([])
const areas = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const batchVisible = ref(false)
const overview = ref({})
const formRef = ref()

const query = reactive({ page: 1, size: 20, areaId: null, type: null, status: null })
const form = reactive({ id: null, areaId: null, spaceNo: '', type: 0, remark: '' })
const batchForm = reactive({ areaId: null, type: 0, nos: '' })

const rules = {
  areaId: [{ required: true, message: '请选择区域', trigger: 'change' }],
  spaceNo: [{ required: true, message: '请输入车位编号', trigger: 'blur' }]
}

const stats = computed(() => [
  { label: '总车位', value: overview.value.total || 0, color: '#303133' },
  { label: '空闲', value: overview.value.free || 0, color: '#67c23a' },
  { label: '占用', value: overview.value.occupied || 0, color: '#e6a23c' },
  { label: '禁用', value: overview.value.disabled || 0, color: '#909399' },
  { label: '维护', value: overview.value.maintaining || 0, color: '#f56c6c' }
])

const typeText = (t) => ({ 0: '普通', 1: '充电', 2: '无障碍', 3: 'VIP' }[t] || '-')
const statusText = (s) => ({ 0: '空闲', 1: '占用', 2: '禁用', 3: '维护' }[s] || '-')
const statusTag = (s) => ({ 0: 'success', 1: 'warning', 2: 'info', 3: 'danger' }[s] || 'info')

const load = async () => {
  loading.value = true
  try {
    const data = await getSpaces(query)
    records.value = data.records
    total.value = data.total
    overview.value = await getSpaceOverview()
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  Object.assign(form, { id: null, areaId: null, spaceNo: '', type: 0, remark: '' })
  dialogVisible.value = true
}

const openEdit = (row) => {
  Object.assign(form, { id: row.id, areaId: row.areaId, spaceNo: row.spaceNo, type: row.type, remark: row.remark })
  dialogVisible.value = true
}

const onSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateSpace(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await createSpace(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const openBatch = () => {
  batchForm.areaId = null
  batchForm.type = 0
  batchForm.nos = ''
  batchVisible.value = true
}

const onBatchSave = async () => {
  if (!batchForm.areaId) {
    ElMessage.warning('请选择区域')
    return
  }
  const nos = batchForm.nos.split('\n').map((s) => s.trim()).filter(Boolean)
  if (!nos.length) {
    ElMessage.warning('请输入车位编号')
    return
  }
  saving.value = true
  try {
    await batchCreateSpace({ areaId: batchForm.areaId, spaceNos: nos, type: batchForm.type })
    ElMessage.success(`成功新增 ${nos.length} 个车位`)
    batchVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const onStatus = async (row, cmd) => {
  await changeSpaceStatus(row.id, { status: Number(cmd) })
  ElMessage.success('状态已调整')
  load()
}

const onDelete = (row) => {
  ElMessageBox.confirm(`确定删除车位「${row.spaceNo}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteSpace(row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

onMounted(async () => {
  areas.value = await getAreas()
  await load()
})
</script>

<style scoped>
.stat-card {
  text-align: center;
}

.stat-title {
  color: #909399;
  font-size: 13px;
}

.stat-value {
  font-size: 26px;
  font-weight: bold;
  margin-top: 6px;
}

.card-title {
  font-weight: 600;
}
</style>
