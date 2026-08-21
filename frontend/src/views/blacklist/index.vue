<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span class="card-title">黑名单管理</span>
        <el-button type="danger" @click="dialogVisible = true">加入黑名单</el-button>
      </div>
    </template>

    <div style="margin-bottom: 12px; display: flex; gap: 8px">
      <el-input v-model="query.plateNo" placeholder="按车牌查询" clearable style="width: 220px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="records" v-loading="loading">
      <el-table-column prop="plateNo" label="车牌" width="140" />
      <el-table-column prop="reason" label="原因" />
      <el-table-column prop="createTime" label="加入时间" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="danger" @click="onRemove(row)">解除</el-button>
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

    <el-dialog v-model="dialogVisible" title="加入黑名单" width="420px">
      <el-form label-width="80px">
        <el-form-item label="车牌号">
          <el-input v-model="form.plateNo" placeholder="如：京A00000" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="如：恶意欠费 / 违规停车" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="onSave">加入</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
defineOptions({ name: 'Blacklist' })
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBlacklist, createBlacklist, deleteBlacklist } from '@/api'

const records = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const query = reactive({ page: 1, size: 10, plateNo: '' })
const form = reactive({ plateNo: '', reason: '' })

const load = async () => {
  loading.value = true
  try {
    const data = await getBlacklist(query)
    records.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const onSave = async () => {
  if (!form.plateNo.trim()) {
    ElMessage.warning('请输入车牌号')
    return
  }
  saving.value = true
  try {
    await createBlacklist({ plateNo: form.plateNo, reason: form.reason })
    ElMessage.success('已加入黑名单')
    dialogVisible.value = false
    form.plateNo = ''
    form.reason = ''
    load()
  } finally {
    saving.value = false
  }
}

const onRemove = (row) => {
  ElMessageBox.confirm(`确定解除车牌 ${row.plateNo} 的黑名单吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteBlacklist(row.id)
      ElMessage.success('已解除')
      load()
    })
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.card-title {
  font-weight: 600;
}
</style>
