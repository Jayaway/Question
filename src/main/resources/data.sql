-- 启动时自动插入测试数据（spring.sql.init.mode=always）
-- 重复插入会被 spring.sql.init.continue-on-error=true 吞掉，不会导致启动失败

-- 默认管理员账号：admin / admin123
-- 密码哈希值来自文档 2.4 节（BCrypt 加密后存储）
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

