<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span class="card-title">停车区域</span>
        <el-button type="primary" @click="openDialog()">新增区域</el-button>
      </div>
    </template>

    <el-table :data="areas" v-loading="loading">
      <el-table-column prop="name" label="区域名" width="120" />
      <el-table-column prop="location" label="位置" />
      <el-table-column prop="spaceCount" label="车位数" width="90" />
      <el-table-column label="收费规则" min-width="140">
        <template #default="{ row }">
          <el-tag v-if="row.billingRuleId" size="small" type="primary">{{ row.billingRuleName || '未知规则' }}</el-tag>
          <el-tag v-else size="small" type="info">全局默认</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑区域' : '新增区域'" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="区域名" prop="name">
          <el-input v-model="form.name" placeholder="如：A区" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" placeholder="如：地面一层东侧" />
        </el-form-item>
        <el-form-item label="车位数">
          <el-input-number v-model="form.spaceCount" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="收费规则">
          <el-select v-model="form.billingRuleId" clearable placeholder="不选=使用全局默认规则" style="width: 100%">
            <el-option v-for="r in billingRules" :key="r.id" :value="r.id" :label="ruleLabel(r)" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
defineOptions({ name: 'SpaceArea' })
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAreas, createArea, updateArea, deleteArea, getBillingRules } from '@/api'

const areas = ref([])
const billingRules = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()

const form = reactive({ id: null, name: '', location: '', spaceCount: 0, billingRuleId: null, sort: 0, status: 1 })

const rules = {
  name: [{ required: true, message: '请输入区域名', trigger: 'blur' }]
}

const ruleLabel = (r) => r.name

const load = async () => {
  loading.value = true
  try {
    const [areaList, ruleList] = await Promise.all([getAreas(), getBillingRules()])
    areas.value = areaList
    billingRules.value = ruleList
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  if (row) {
    Object.assign(form, { id: row.id, name: row.name, location: row.location, spaceCount: row.spaceCount, billingRuleId: row.billingRuleId ?? null, sort: row.sort, status: row.status })
  } else {
    Object.assign(form, { id: null, name: '', location: '', spaceCount: 0, billingRuleId: null, sort: 0, status: 1 })
  }
  dialogVisible.value = true
}

const onSave = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateArea(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await createArea(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const onDelete = (row) => {
  ElMessageBox.confirm(`确定删除区域「${row.name}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteArea(row.id)
      ElMessage.success('删除成功')
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
