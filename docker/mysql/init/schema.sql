-- ===============================
--  算法定义表
-- ===============================
CREATE TABLE algorithm_definitions
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    algorithm_code VARCHAR(64)  NOT NULL UNIQUE COMMENT '算法标识符',
    algorithm_name VARCHAR(128) NOT NULL COMMENT '算法名称（可修改）',
    status         VARCHAR(10)  NOT NULL DEFAULT 'INACTIVE' COMMENT '启动状态：INACTIVE-关闭，ACTIVE-启动',
    version_id     BIGINT       NOT NULL COMMENT '版本号（对应版本ID）',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算法定义表';


-- ===============================
--  算法版本表
-- ===============================
CREATE TABLE algorithm_versions
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    version_name      VARCHAR(128) NOT NULL COMMENT '版本名称',
    description       VARCHAR(256) NOT NULL COMMENT '描述（可修改）',
    definition_id     BIGINT       NOT NULL COMMENT '算法定义ID',
    status            VARCHAR(10)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT-训练 RELEASE-发布',
    version           INT          NOT NULL COMMENT '版本号',
    model_storage_key VARCHAR(256) NOT NULL COMMENT '模型存储索引（服务器 .pt 文件路径或对象存储Key）',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_version_name (version_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算法版本表';


-- ===============================
--  边缘设备表
-- ===============================
CREATE TABLE devices
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    device_code      VARCHAR(64) NOT NULL UNIQUE COMMENT '边缘设备编号（可修改）',
    apply_region     VARCHAR(128) COMMENT '应用区域（可修改）',
    location         VARCHAR(256) COMMENT '所在位置（可修改）',
    status           VARCHAR(10) NOT NULL DEFAULT 'INACTIVE' COMMENT '启动状态：INACTIVE-关闭，ACTIVE-启动',
    product_model    VARCHAR(128) COMMENT '产品型号',
    chip             VARCHAR(128) COMMENT '搭载芯片',
    cpu              VARCHAR(128) COMMENT 'CPU型号',
    ai_compute_power VARCHAR(128) COMMENT '峰值算力（TOPS）',
    os               VARCHAR(128) COMMENT '操作系统',
    updated_by       VARCHAR(64) COMMENT '更新用户',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='边缘设备信息表';


-- ===============================
--  设备算法关系表
-- ===============================
CREATE TABLE device_algorithm_rel
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    algorithm_id BIGINT   NOT NULL COMMENT '算法ID',
    device_id    BIGINT   NOT NULL COMMENT '设备ID',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_device_algorithm (device_id, algorithm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备算法关系表';


-- ===============================
--  区域表（支持多级）
-- ===============================
CREATE TABLE regions
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    region_code  VARCHAR(64)  NOT NULL UNIQUE COMMENT '区域编码（唯一标识）',
    region_name  VARCHAR(128) NOT NULL COMMENT '区域名称',
    parent_id    BIGINT                DEFAULT NULL COMMENT '父区域ID',
    region_level TINYINT      NOT NULL DEFAULT 1 COMMENT '区域层级：1-省/大区，2-市，3-区县，4-站点',
    description  VARCHAR(256) COMMENT '区域描述说明',
    updated_by   VARCHAR(64) COMMENT '更新用户',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用区域表（支持多级区域管理）';


-- ===============================
--  区域设备关系表
-- ===============================
CREATE TABLE region_device_rel
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    region_id  BIGINT   NOT NULL COMMENT '区域ID',
    device_id  BIGINT   NOT NULL COMMENT '设备ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_region_device (region_id, device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域设备关系表';


-- ===============================
--  设备运行监控指标
-- ===============================
CREATE TABLE device_metrics
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    device_id      BIGINT   NOT NULL COMMENT '设备ID',
    cpu_usage      DECIMAL(5, 2) COMMENT 'CPU使用率（0-100）',
    memory_usage   DECIMAL(5, 2) COMMENT '内存使用率（0-100）',
    gpu_usage      DECIMAL(5, 2) COMMENT 'GPU使用率（0-100）',
    disk_usage     DECIMAL(5, 2) COMMENT '磁盘使用率（0-100）',
    network_status VARCHAR(128) COMMENT '网络状况（如：good / poor / offline / RTTxx）',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='边缘设备运行记录表';
