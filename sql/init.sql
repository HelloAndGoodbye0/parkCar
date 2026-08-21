-- ============================================================
-- 停车场停车管理系统 建库建表脚本
-- 数据库：MySQL 8.0
-- ============================================================
CREATE DATABASE IF NOT EXISTS park_car DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE park_car;
-- 保证后续建表/插入语句按 UTF-8 解析（Docker 官方镜像 entrypoint 导入时默认 latin1 会导致中文乱码）
SET NAMES utf8mb4;

-- 通用字段说明（本系统统一约定）：
--   id          BIGINT 自增主键
--   create_time 创建时间
--   update_time 更新时间
--   deleted     逻辑删除 0=正常 1=删除

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS parking_area;
DROP TABLE IF EXISTS parking_space;
DROP TABLE IF EXISTS billing_rule;
DROP TABLE IF EXISTS parking_record;
DROP TABLE IF EXISTS billing_order;
DROP TABLE IF EXISTS payment_record;
DROP TABLE IF EXISTS membership_package;
DROP TABLE IF EXISTS membership_card;
DROP TABLE IF EXISTS blacklist;
DROP TABLE IF EXISTS daily_report;

-- ============================================================
-- 1. 系统用户
-- ============================================================
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '登录名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
    real_name   VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT = '系统用户';

-- ============================================================
-- 2. 角色
-- ============================================================
CREATE TABLE sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    code        VARCHAR(50)  NOT NULL COMMENT '角色编码 ADMIN/OPERATOR',
    name        VARCHAR(50)  NOT NULL COMMENT '角色名',
    remark      VARCHAR(200) DEFAULT NULL COMMENT '备注',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB COMMENT = '角色';

-- ============================================================
-- 3. 用户-角色关联
-- ============================================================
CREATE TABLE sys_user_role (
    id      BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE = InnoDB COMMENT = '用户-角色关联';

-- ============================================================
-- 4. 操作日志
-- ============================================================
CREATE TABLE operation_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       DEFAULT NULL COMMENT '操作人ID',
    username    VARCHAR(50)  DEFAULT NULL COMMENT '操作人账号',
    module      VARCHAR(50)  DEFAULT NULL COMMENT '模块',
    action      VARCHAR(50)  DEFAULT NULL COMMENT '操作',
    content     VARCHAR(500) DEFAULT NULL COMMENT '操作内容',
    ip          VARCHAR(50)  DEFAULT NULL COMMENT '来源IP',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB COMMENT = '操作日志';

-- ============================================================
-- 5. 停车区域
-- ============================================================
CREATE TABLE parking_area (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(50)  NOT NULL COMMENT '区域名',
    location        VARCHAR(100) DEFAULT NULL COMMENT '位置描述',
    space_count     INT          NOT NULL DEFAULT 0 COMMENT '车位数量(冗余)',
    billing_rule_id BIGINT       DEFAULT NULL COMMENT '绑定的收费规则ID NULL=使用全局默认规则',
    sort            INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '停车区域';

-- ============================================================
-- 6. 车位
-- ============================================================
CREATE TABLE parking_space (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    area_id     BIGINT       NOT NULL COMMENT '所属区域ID',
    space_no    VARCHAR(20)  NOT NULL COMMENT '车位编号 A-001',
    type        TINYINT      NOT NULL DEFAULT 0 COMMENT '0=普通 1=充电 2=无障碍 3=VIP',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0=空闲 1=占用 2=禁用 3=维护',
    remark      VARCHAR(200) DEFAULT NULL COMMENT '备注',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_space_no (space_no),
    KEY idx_area_status (area_id, status)
) ENGINE = InnoDB COMMENT = '车位';

-- ============================================================
-- 7. 收费规则
-- ============================================================
CREATE TABLE billing_rule (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    name           VARCHAR(50)   NOT NULL COMMENT '规则名',
    rule_type      TINYINT       NOT NULL DEFAULT 0 COMMENT '0=按时 1=按次',
    free_minutes   INT           NOT NULL DEFAULT 0 COMMENT '免费时长(分钟)',
    first_hour_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '首小时/单次费用',
    hourly_fee     DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '之后每小时费用',
    max_daily_fee  DECIMAL(10,2) DEFAULT NULL COMMENT '每日封顶价 NULL=无封顶',
    night_start    TIME          DEFAULT NULL COMMENT '夜间计费开始',
    night_end      TIME          DEFAULT NULL COMMENT '夜间计费结束',
    night_fee      DECIMAL(10,2) DEFAULT NULL COMMENT '夜间费用',
    is_default     TINYINT       NOT NULL DEFAULT 0 COMMENT '1=全局默认(兜底)规则,全局至多一条',
    version        INT           NOT NULL DEFAULT 1 COMMENT '规则版本',
    remark         VARCHAR(200)  DEFAULT NULL,
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '收费规则';

-- ============================================================
-- 8. 停车记录（在场/历史）
-- ============================================================
CREATE TABLE parking_record (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    plate_no         VARCHAR(20)   NOT NULL COMMENT '车牌号',
    space_id         BIGINT        DEFAULT NULL COMMENT '占用车位ID',
    area_id          BIGINT        DEFAULT NULL COMMENT '区域ID(冗余)',
    in_time          DATETIME      NOT NULL COMMENT '入场时间',
    out_time         DATETIME      DEFAULT NULL COMMENT '出场时间',
    status           TINYINT       NOT NULL DEFAULT 0 COMMENT '0=在场 1=已离场 2=异常',
    is_member        TINYINT       NOT NULL DEFAULT 0 COMMENT '是否月卡车辆',
    card_id          BIGINT        DEFAULT NULL COMMENT '关联月卡ID',
    billing_rule_id  BIGINT        DEFAULT NULL COMMENT '计费生效规则',
    charge_amount    DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '应收金额',
    paid_amount      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实收金额',
    discount_amount  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '减免金额',
    operator_in      BIGINT        DEFAULT NULL COMMENT '入场操作人',
    operator_out     BIGINT        DEFAULT NULL COMMENT '出场操作人',
    remark           VARCHAR(200)  DEFAULT NULL,
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_plate_no (plate_no),
    KEY idx_status (status),
    KEY idx_in_time (in_time),
    KEY idx_space_id (space_id)
) ENGINE = InnoDB COMMENT = '停车记录';

-- ============================================================
-- 9. 收费订单
-- ============================================================
CREATE TABLE billing_order (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    order_no    VARCHAR(32)   NOT NULL COMMENT '订单号',
    record_id   BIGINT        DEFAULT NULL COMMENT '停车记录ID(月卡订单为空)',
    area_id     BIGINT        DEFAULT NULL COMMENT '区域ID(冗余,出场结算时写入,用于按区域过滤订单)',
    plate_no    VARCHAR(20)   DEFAULT NULL COMMENT '车牌(冗余)',
    amount      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '应收金额',
    discount    DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '减免金额',
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实收金额',
    pay_type    TINYINT       DEFAULT NULL COMMENT '1=现金 2=微信 3=支付宝 4=月卡抵扣',
    status      TINYINT       NOT NULL DEFAULT 0 COMMENT '0=待支付 1=已支付 2=已取消',
    operator_id BIGINT        DEFAULT NULL COMMENT '操作收费员',
    pay_time    DATETIME      DEFAULT NULL COMMENT '支付时间',
    remark      VARCHAR(200)  DEFAULT NULL,
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_record_id (record_id),
    KEY idx_plate_no (plate_no),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB COMMENT = '收费订单';

-- ============================================================
-- 10. 支付流水
-- ============================================================
CREATE TABLE payment_record (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    order_id       BIGINT        NOT NULL COMMENT '订单ID',
    pay_type       TINYINT       NOT NULL COMMENT '1=现金 2=微信 3=支付宝',
    amount         DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
    transaction_no VARCHAR(64)   DEFAULT NULL COMMENT '第三方流水号',
    status         TINYINT       NOT NULL DEFAULT 1 COMMENT '1=成功 0=失败',
    operator_id    BIGINT        DEFAULT NULL COMMENT '操作人',
    pay_time       DATETIME      DEFAULT NULL COMMENT '支付时间',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id)
) ENGINE = InnoDB COMMENT = '支付流水';

-- ============================================================
-- 11. 会员套餐
-- ============================================================
CREATE TABLE membership_package (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    name          VARCHAR(50)   NOT NULL COMMENT '套餐名',
    duration_days INT           NOT NULL COMMENT '有效期天数',
    price         DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '价格',
    status        TINYINT       NOT NULL DEFAULT 1 COMMENT '1=上架 0=下架',
    start_time    DATETIME      DEFAULT NULL COMMENT '活动开始时间，NULL=长期有效',
    end_time      DATETIME      DEFAULT NULL COMMENT '活动结束时间，NULL=长期有效',
    remark        VARCHAR(200)  DEFAULT NULL,
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '会员套餐';

-- ============================================================
-- 12. 月卡（车辆会员）
-- ============================================================
CREATE TABLE membership_card (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    plate_no     VARCHAR(20)   NOT NULL COMMENT '绑定车牌',
    package_id   BIGINT        DEFAULT NULL COMMENT '套餐ID',
    owner_name   VARCHAR(50)   DEFAULT NULL COMMENT '车主姓名',
    owner_phone  VARCHAR(20)   DEFAULT NULL COMMENT '联系电话',
    start_time   DATETIME      NOT NULL COMMENT '生效时间',
    end_time     DATETIME      NOT NULL COMMENT '到期时间',
    status       TINYINT       NOT NULL DEFAULT 1 COMMENT '1=有效 0=过期 2=已退订',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_plate_status (plate_no, status),
    KEY idx_end_time (end_time)
) ENGINE = InnoDB COMMENT = '月卡';

-- ============================================================
-- 13. 黑名单
-- ============================================================
CREATE TABLE blacklist (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    plate_no    VARCHAR(20)  NOT NULL COMMENT '车牌',
    reason      VARCHAR(200) DEFAULT NULL COMMENT '原因',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1=生效 0=解除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT       DEFAULT NULL COMMENT '操作人',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_plate_no (plate_no)
) ENGINE = InnoDB COMMENT = '黑名单';

-- ============================================================
-- 14. 每日营收汇总
-- ============================================================
CREATE TABLE daily_report (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    report_date      DATE          NOT NULL COMMENT '统计日期',
    total_amount     DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '总营收',
    order_count      INT           NOT NULL DEFAULT 0 COMMENT '订单笔数',
    cash_amount      DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '现金金额',
    wechat_amount    DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '微信金额',
    alipay_amount    DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '支付宝金额',
    card_amount      DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '月卡抵扣金额',
    in_count         INT           NOT NULL DEFAULT 0 COMMENT '入场车次',
    out_count        INT           NOT NULL DEFAULT 0 COMMENT '出场车次',
    avg_duration_min INT           NOT NULL DEFAULT 0 COMMENT '平均停车时长(分钟)',
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_date (report_date)
) ENGINE = InnoDB COMMENT = '每日营收汇总';

-- ============================================================
-- 15. 用户-区域关联（数据权限：收费员负责管理的停车区域）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user_area (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    area_id     BIGINT   NOT NULL COMMENT '区域ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_area (user_id, area_id),
    KEY idx_area_id (area_id)
) ENGINE = InnoDB COMMENT = '用户-区域关联(数据权限)';

SET FOREIGN_KEY_CHECKS = 1;
