import request from '@/utils/request'

// 认证
export const login = (data) => request.post('/auth/login', data)
export const getMe = () => request.get('/auth/me')

// 用户
export const getUsers = (params) => request.get('/users', { params })
export const createUser = (data) => request.post('/users', data)
export const updateUser = (id, data) => request.put(`/users/${id}`, data)
export const changeUserStatus = (id, data) => request.put(`/users/${id}/status`, data)
export const resetUserPassword = (id, data) => request.put(`/users/${id}/password`, data)
export const deleteUser = (id) => request.delete(`/users/${id}`)
export const getRoles = () => request.get('/users/roles')

// 操作日志
export const getLogs = (params) => request.get('/logs', { params })

// 区域
export const getAreas = () => request.get('/areas')
export const createArea = (data) => request.post('/areas', data)
export const updateArea = (id, data) => request.put(`/areas/${id}`, data)
export const deleteArea = (id) => request.delete(`/areas/${id}`)

// 车位
export const getSpaces = (params) => request.get('/spaces', { params })
export const getSpaceOverview = () => request.get('/spaces/overview')
export const createSpace = (data) => request.post('/spaces', data)
export const batchCreateSpace = (data) => request.post('/spaces/batch', data)
export const updateSpace = (id, data) => request.put(`/spaces/${id}`, data)
export const deleteSpace = (id) => request.delete(`/spaces/${id}`)
export const changeSpaceStatus = (id, data) => request.put(`/spaces/${id}/status`, data)

// 出入场
export const vehicleIn = (data) => request.post('/records/in', data)
export const getCurrentRecords = (params) => request.get('/records/current', { params })
export const getHistoryRecords = (params) => request.get('/records', { params })
export const outPreview = (data) => request.post('/records/out/preview', data)
export const outSettle = (data) => request.post('/records/out/settle', data)
export const manualOut = (id, data) => request.post(`/records/${id}/manual-out`, data)

// 收费规则
export const getBillingRules = () => request.get('/billing-rules')
export const getActiveRule = (areaId) => request.get('/billing-rules/active', { params: { areaId } })
export const createBillingRule = (data) => request.post('/billing-rules', data)
export const setDefaultBillingRule = (id) => request.post(`/billing-rules/${id}/default`)
export const updateBillingRule = (id, data) => request.put(`/billing-rules/${id}`, data)
export const deleteBillingRule = (id) => request.delete(`/billing-rules/${id}`)

// 订单
export const getOrders = (params) => request.get('/orders', { params })
export const getOrderDetail = (orderNo) => request.get(`/orders/${orderNo}`)

// 会员月卡
export const getPackages = (params) => request.get('/membership/packages', { params })
export const createPackage = (data) => request.post('/membership/packages', data)
export const updatePackage = (id, data) => request.put(`/membership/packages/${id}`, data)
export const getCards = (params) => request.get('/membership/cards', { params })
export const getCardByPlate = (plateNo) => request.get(`/membership/cards/plate/${plateNo}`)
export const createCard = (data) => request.post('/membership/cards', data)
export const renewCard = (id, data) => request.post(`/membership/cards/${id}/renew`, data)
export const cancelCard = (id) => request.delete(`/membership/cards/${id}`)

// 黑名单
export const getBlacklist = (params) => request.get('/blacklist', { params })
export const createBlacklist = (data) => request.post('/blacklist', data)
export const deleteBlacklist = (id) => request.delete(`/blacklist/${id}`)

// 报表
export const getRevenueReport = (params) => request.get('/reports/revenue', { params })
export const getTrafficReport = (params) => request.get('/reports/traffic', { params })
export const getOccupancyReport = () => request.get('/reports/occupancy')
