<template>
  <el-card shadow="never">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span class="card-title">收费规则</span>
        <el-button type="primary" @click="openCreate">新增规则</el-button>
      </div>
    </template>

    <el-alert
      type="info"
      :closable="false"
      title="规则可多条并存，各区域在「区域管理」中绑定收费规则；未绑定区域的车辆按「全局默认」规则计费。全局默认规则至多一条。"
      style="margin-bottom: 12px"
    />

    <el-table :data="rules" v-loading="loading">
      <el-table-column prop="name" label="规则名" width="130" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">{{ row.ruleType === 0 ? '按时' : '按次' }}</template>
      </el-table-column>
      <el-table-column prop="freeMinutes" label="免费(分)" width="90" />
      <el-table-column prop="firstHourFee" label="首小时(元)" width="100" />
      <el-table-column prop="hourlyFee" label="每小时(元)" width="100" />
      <el-table-column label="封顶(元)" width="90">
        <template #default="{ row }">{{ row.maxDailyFee ?? '无' }}</template>
      </el-table-column>
      <el-table-column label="夜间计费" width="140">
        <template #default="{ row }">
          <template v-if="row.ruleType === 0 && row.nightStart && row.nightEnd && row.nightFee != null">
            {{ row.nightStart.slice(0, 5) }}~{{ row.nightEnd.slice(0, 5) }} ¥{{ row.nightFee }}
          </template>
          <span v-else style="color: #909399">未启用</span>
        </template>
      </el-table-column>
      <el-table-column prop="version" label="版本" width="70" />
      <el-table-column label="默认规则" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault === 1" type="warning" size="small">全局默认</el-tag>
          <span v-else style="color: #909399">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="操作" width="230">
        <template #default="{ row }">
          <el-button v-if="row.isDefault !== 1" link type="warning" @click="onSetDefault(row)">设为默认</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑规则' : '新增规则'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="规则名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="计费类型">
          <el-radio-group v-model="form.ruleType">
            <el-radio-button :value="0">按时计费</el-radio-button>
            <el-radio-button :value="1">按次计费</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="免费时长(分)">
          <el-input-number v-model="form.freeMinutes" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="form.ruleType === 0 ? '首小时费用' : '单次费用'">
          <el-input-number v-model="form.firstHourFee" :min="0" :precision="2" :step="0.5" style="width: 100%" />
        </el-form-item>
        <el-form-item label="每小时费用" v-if="form.ruleType === 0">
          <el-input-number v-model="form.hourlyFee" :min="0" :precision="2" :step="0.5" style="width: 100%" />
        </el-form-item>
        <el-form-item label="每日封顶" v-if="form.ruleType === 0">
          <el-input-number v-model="form.maxDailyFee" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <template v-if="form.ruleType === 0">
          <el-divider content-position="left">夜间计费（可选）</el-divider>
          <el-form-item label="启用夜间计费">
            <el-switch v-model="nightEnabled" />
          </el-form-item>
          <template v-if="nightEnabled">
            <el-form-item label="夜间时段" required>
              <div style="display: flex; align-items: center; width: 100%; gap: 8px">
                <el-time-picker v-model="form.nightStart" value-format="HH:mm" format="HH:mm" placeholder="开始" style="flex: 1" />
                <span>~</span>
                <el-time-picker v-model="form.nightEnd" value-format="HH:mm" format="HH:mm" placeholder="结束" style="flex: 1" />
              </div>
              <div style="color: #909399; font-size: 12px; line-height: 1.5; margin-top: 4px">
                支持跨天时段（如 22:00~06:00）。车辆停留每覆盖一个夜间时段计一笔夜间费用，其余白天时长按上方按时规则计费，每日封顶仅约束白天部分。
              </div>
            </el-form-item>
            <el-form-item label="夜间费用(元)">
              <el-input-number v-model="form.nightFee" :min="0" :precision="2" :step="0.5" style="width: 100%" />
            </el-form-item>
          </template>
        </template>
        <el-form-item label="备注">
          <el-input v-model="form.remark" />
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
defineOptions({ name: 'BillingRule' })
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBillingRules, createBillingRule, updateBillingRule, setDefaultBillingRule, deleteBillingRule } from '@/api'

const rules = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()

const emptyForm = () => ({
  id: null, name: '', ruleType: 0, freeMinutes: 15,
  firstHourFee: 5, hourlyFee: 3, maxDailyFee: 30,
  nightStart: null, nightEnd: null, nightFee: null,
  remark: ''
})
const form = reactive(emptyForm())
const nightEnabled = ref(false)

const formRules = {
  name: [{ required: true, message: '请输入规则名', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    rules.value = await getBillingRules()
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  Object.assign(form, emptyForm())
  nightEnabled.value = false
  dialogVisible.value = true
}

const openEdit = (row) => {
  Object.assign(form, {
    id: row.id, name: row.name, ruleType: row.ruleType, freeMinutes: row.freeMinutes,
    firstHourFee: row.firstHourFee, hourlyFee: row.hourlyFee, maxDailyFee: row.maxDailyFee,
    nightStart: row.nightStart ? row.nightStart.slice(0, 5) : null,
    nightEnd: row.nightEnd ? row.nightEnd.slice(0, 5) : null,
    nightFee: row.nightFee ?? null,
    remark: row.remark
  })
  nightEnabled.value = !!(row.nightStart && row.nightEnd && row.nightFee != null)
  dialogVisible.value = true
}

const onSave = async () => {
  await formRef.value.validate()
  if (nightEnabled.value) {
    if (!form.nightStart || !form.nightEnd) {
      ElMessage.warning('请选择夜间计费时段')
      return
    }
    if (form.nightStart === form.nightEnd) {
      ElMessage.warning('夜间计费的开始与结束时间不能相同')
      return
    }
    if (form.nightFee == null) {
      ElMessage.warning('请输入夜间费用')
      return
    }
  }
  saving.value = true
  try {
    const payload = {
      ...form,
      nightStart: nightEnabled.value && form.nightStart ? `${form.nightStart}:00` : null,
      nightEnd: nightEnabled.value && form.nightEnd ? `${form.nightEnd}:00` : null,
      nightFee: nightEnabled.value ? form.nightFee : null
    }
    if (form.id) {
      await updateBillingRule(form.id, payload)
      ElMessage.success('已保存')
    } else {
      await createBillingRule(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const onSetDefault = (row) => {
  ElMessageBox.confirm(`确定将规则「${row.name}」设为全局默认吗？未绑定具体规则的区域将按此规则计费。`, '提示', { type: 'warning' })
    .then(async () => {
      await setDefaultBillingRule(row.id)
      ElMessage.success('已设为全局默认')
      load()
    })
    .catch(() => {})
}

const onDelete = (row) => {
  ElMessageBox.confirm(`确定删除规则「${row.name}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteBillingRule(row.id)
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
