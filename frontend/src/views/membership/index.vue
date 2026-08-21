<template>
  <el-row :gutter="16">
    <!-- 套餐 -->
    <el-col :span="8">
      <el-card shadow="never">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center">
            <span class="card-title">会员套餐</span>
            <el-button v-if="userStore.isAdmin" type="primary" size="small" @click="openPackage">新增套餐</el-button>
          </div>
        </template>
        <el-table :data="packages" v-loading="packageLoading">
          <el-table-column prop="name" label="套餐名" width="90" />
          <el-table-column prop="durationDays" label="天数" width="70" />
          <el-table-column label="价格" width="90">
            <template #default="{ row }">
              <span style="color: #f56c6c; font-weight: bold">¥{{ row.price }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="70">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '上架' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" v-if="userStore.isAdmin">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPackage(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card shadow="never" style="margin-top: 16px">
        <template #header>
          <span class="card-title">办理 / 续费月卡</span>
        </template>
        <el-form label-width="80px">
          <el-form-item label="车牌号">
            <el-input v-model="cardForm.plateNo" placeholder="如：京B88888" @blur="queryCard" />
          </el-form-item>
          <el-form-item label="车主姓名">
            <el-input v-model="cardForm.ownerName" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="cardForm.ownerPhone" />
          </el-form-item>
          <el-form-item label="选择套餐">
            <el-select v-model="cardForm.packageId" style="width: 100%">
              <el-option v-for="p in enabledPackages" :key="p.id" :label="`${p.name} ¥${p.price}`" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="cardSaving" @click="onCreateCard">
              {{ existingCard ? '续费月卡' : '办理月卡' }}
            </el-button>
          </el-form-item>
        </el-form>
        <el-alert
          v-if="existingCard"
          type="success"
          :closable="false"
          :title="`该车现有有效月卡，到期时间：${existingCard.endTime}`"
          style="margin-top: 8px"
        />
      </el-card>
    </el-col>

    <!-- 月卡列表 -->
    <el-col :span="16">
      <el-card shadow="never">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center">
            <span class="card-title">月卡列表</span>
            <div style="display: flex; gap: 8px">
              <el-input v-model="cardQuery.plateNo" placeholder="按车牌查询" clearable style="width: 180px" @keyup.enter="loadCards" />
              <el-button type="primary" @click="loadCards">查询</el-button>
            </div>
          </div>
        </template>
        <el-table :data="cards" v-loading="cardLoading">
          <el-table-column prop="plateNo" label="车牌" width="110" />
          <el-table-column prop="ownerName" label="车主" width="90" />
          <el-table-column prop="ownerPhone" label="电话" width="120" />
          <el-table-column prop="packageName" label="套餐" width="80" />
          <el-table-column prop="startTime" label="生效" />
          <el-table-column prop="endTime" label="到期" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '有效' : row.status === 2 ? '退订' : '过期' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 1"
                link
                type="primary"
                @click="onRenewCard(row)"
              >
                续费
              </el-button>
              <el-button
                v-if="row.status === 1 && userStore.isAdmin"
                link
                type="danger"
                @click="onCancelCard(row)"
              >
                退订
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="cardQuery.page"
          :page-size="cardQuery.size"
          :total="cardTotal"
          layout="total, prev, pager, next"
          style="margin-top: 12px; justify-content: flex-end"
          @current-change="loadCards"
        />
      </el-card>
    </el-col>
  </el-row>

  <!-- 套餐编辑 -->
  <el-dialog v-model="packageVisible" :title="pkgForm.id ? '编辑套餐' : '新增套餐'" width="460px">
    <el-form label-width="90px">
      <el-form-item label="套餐名">
        <el-input v-model="pkgForm.name" />
      </el-form-item>
      <el-form-item label="有效期(天)">
        <el-input-number v-model="pkgForm.durationDays" :min="1" style="width: 100%" />
      </el-form-item>
      <el-form-item label="价格(元)">
        <el-input-number v-model="pkgForm.price" :min="0" :precision="2" style="width: 100%" />
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="pkgForm.status" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="packageVisible = false">取消</el-button>
      <el-button type="primary" @click="onSavePackage">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
defineOptions({ name: 'Membership' })
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getPackages, createPackage, updatePackage,
  getCards, getCardByPlate, createCard, renewCard, cancelCard
} from '@/api'

const userStore = useUserStore()
const packages = ref([])
const packageLoading = ref(false)
// 办理/续费下拉仅展示上架套餐
const enabledPackages = computed(() => packages.value.filter((p) => p.status === 1))
const cards = ref([])
const cardTotal = ref(0)
const cardLoading = ref(false)
const cardSaving = ref(false)
const packageVisible = ref(false)
const existingCard = ref(null)

const cardForm = reactive({ plateNo: '', ownerName: '', ownerPhone: '', packageId: null })
const cardQuery = reactive({ page: 1, size: 10, plateNo: '' })
const pkgForm = reactive({ id: null, name: '', durationDays: 30, price: 300, status: 1 })

const loadPackages = async () => {
  packageLoading.value = true
  try {
    packages.value = await getPackages({ all: true })
  } finally {
    packageLoading.value = false
  }
}

const loadCards = async () => {
  cardLoading.value = true
  try {
    const data = await getCards(cardQuery)
    cards.value = data.records
    cardTotal.value = data.total
  } finally {
    cardLoading.value = false
  }
}

const queryCard = async () => {
  if (!cardForm.plateNo.trim()) {
    existingCard.value = null
    return
  }
  const card = await getCardByPlate(cardForm.plateNo.trim())
  existingCard.value = card
}

const onCreateCard = async () => {
  if (!cardForm.plateNo.trim()) {
    ElMessage.warning('请输入车牌号')
    return
  }
  if (!cardForm.packageId) {
    ElMessage.warning('请选择套餐')
    return
  }
  cardSaving.value = true
  try {
    const data = await createCard({
      plateNo: cardForm.plateNo,
      packageId: cardForm.packageId,
      ownerName: cardForm.ownerName,
      ownerPhone: cardForm.ownerPhone
    })
    ElMessage.success(`月卡办理成功，到期时间：${data.endTime}，金额 ¥${data.amount}`)
    cardForm.plateNo = ''
    cardForm.ownerName = ''
    cardForm.ownerPhone = ''
    cardForm.packageId = null
    existingCard.value = null
    loadCards()
  } finally {
    cardSaving.value = false
  }
}

const onRenewCard = (row) => {
  ElMessageBox.confirm(`为车牌 ${row.plateNo} 续费（按所选套餐）？`, '月卡续费', { type: 'warning' })
    .then(async () => {
      const pkg = enabledPackages.value[0]
      if (!pkg) {
        ElMessage.warning('请先创建并启用套餐')
        return
      }
      const data = await renewCard(row.id, { packageId: pkg.id })
      ElMessage.success(`续费成功，新到期时间：${data.endTime}`)
      loadCards()
    })
    .catch(() => {})
}

const onCancelCard = (row) => {
  ElMessageBox.confirm(`确定退订车牌 ${row.plateNo} 的月卡吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await cancelCard(row.id)
      ElMessage.success('已退订')
      loadCards()
    })
    .catch(() => {})
}

const openPackage = (row) => {
  if (row) {
    Object.assign(pkgForm, { id: row.id, name: row.name, durationDays: row.durationDays, price: row.price, status: row.status })
  } else {
    Object.assign(pkgForm, { id: null, name: '', durationDays: 30, price: 300, status: 1 })
  }
  packageVisible.value = true
}

const onSavePackage = async () => {
  if (!pkgForm.name.trim()) {
    ElMessage.warning('请输入套餐名')
    return
  }
  if (pkgForm.id) {
    await updatePackage(pkgForm.id, pkgForm)
    ElMessage.success('修改成功')
  } else {
    await createPackage(pkgForm)
    ElMessage.success('新增成功')
  }
  packageVisible.value = false
  loadPackages()
}

onMounted(async () => {
  await loadPackages()
  await loadCards()
})
</script>

<style scoped>
.card-title {
  font-weight: 600;
}
</style>
