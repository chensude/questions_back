-- 删除已存在的表（如果需要重新创建）
DROP TABLE IF EXISTS questions;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    openid VARCHAR(100) NOT NULL UNIQUE COMMENT '微信openid',
    nickname VARCHAR(100) COMMENT '用户昵称',
    avatar_url TEXT COMMENT '头像URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 创建题目表
CREATE TABLE IF NOT EXISTS questions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT '用户ID',
    question_type VARCHAR(20) NOT NULL COMMENT '题目类型：单选题/多选题/简答题',
    question_text TEXT NOT NULL COMMENT '题目内容',
    options TEXT COMMENT '选项内容，用|||分隔',
    correct_answer TEXT NOT NULL COMMENT '正确答案',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_type (user_id, question_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';