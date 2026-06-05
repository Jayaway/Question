-- ============================================================
-- 在线问卷调查与数据分析系统 - 数据库初始化 SQL
-- 用法：老师手动导入  mysql -u root -p < sql/init.sql
-- 自动建库 + 5 张表 + 测试数据 (1 用户 + 1 问卷 + 3 题)
-- ============================================================

DROP DATABASE IF EXISTS survey_system;
CREATE DATABASE survey_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE survey_system;

-- ===== 1. 用户表 =====
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 2. 问卷表 =====
CREATE TABLE survey (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status TINYINT DEFAULT 0 COMMENT '0-草稿 1-已发布 2-已停止',
    creator_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES sys_user(id),
    INDEX idx_creator (creator_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 3. 题目表 =====
CREATE TABLE question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_id BIGINT NOT NULL,
    type TINYINT NOT NULL COMMENT '1-单选 2-多选 3-文本',
    title VARCHAR(500) NOT NULL,
    options TEXT COMMENT '选项，JSON数组格式 ["A","B","C"]',
    sort_order INT DEFAULT 0,
    required TINYINT DEFAULT 1,
    FOREIGN KEY (survey_id) REFERENCES survey(id) ON DELETE CASCADE,
    INDEX idx_survey (survey_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 4. 答卷表 =====
CREATE TABLE response (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_id BIGINT NOT NULL,
    respondent_ip VARCHAR(50),
    submit_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (survey_id) REFERENCES survey(id) ON DELETE CASCADE,
    INDEX idx_response_survey (survey_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 5. 答案表 =====
CREATE TABLE answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    response_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    FOREIGN KEY (response_id) REFERENCES response(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES question(id),
    INDEX idx_answer_response (response_id),
    INDEX idx_answer_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 测试数据
-- ============================================================

-- 默认管理员：用户名 admin，密码 admin123
-- BCrypt 哈希值与 src/main/resources/data.sql 中的一致
INSERT INTO sys_user (id, username, password, email) VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@test.com');

-- 一份示例问卷（已发布），方便答辩演示
INSERT INTO survey (id, title, description, status, creator_id) VALUES
(1, '校园食堂满意度调查', '本问卷用于收集同学对食堂的意见，共3题，预计1分钟', 1, 1);

-- 3 道题（单选 / 多选 / 文本）
INSERT INTO question (id, survey_id, type, title, options, sort_order, required) VALUES
(1, 1, 1, '您对食堂菜品的口味满意度如何？',
     '["非常满意","满意","一般","不满意"]', 1, 1),
(2, 1, 2, '您最关心食堂哪个方面？',
     '["价格","卫生","菜品口味","服务态度","用餐环境"]', 2, 1),
(3, 1, 3, '您对食堂有什么建议？',
     NULL, 3, 0);
