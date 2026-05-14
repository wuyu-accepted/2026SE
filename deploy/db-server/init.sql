-- ============================================================
-- 数据库服务器初始化脚本
-- 用途：在数据库服务器上创建项目所需的数据库和用户
-- 适用数据库：PostgreSQL 16
-- 使用方式：
--   psql -U postgres -f init.sql
-- 或逐行执行：
--   psql -U postgres -c "CREATE DATABASE ..."
-- ============================================================

-- 1. 创建数据库（如果不存在）
-- 注意：ruc_platform 是项目默认数据库名，
-- 可修改为实际生产数据库名，同时更新应用服务器配置
SELECT 'CREATE DATABASE ruc_platform'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ruc_platform')\gexec

-- 2. 创建专用数据库用户（可选，建议生产环境使用）
-- 如果不需要创建新用户，可跳过此步骤，直接使用 postgres 用户
-- 密码请替换为强密码
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'ruc_admin') THEN
        CREATE ROLE ruc_admin WITH LOGIN PASSWORD 'change_this_to_strong_password';
        GRANT ALL PRIVILEGES ON DATABASE ruc_platform TO ruc_admin;
    END IF;
END
$$;

-- 3. 授予 Schema 权限（连接数据库后执行）
-- 如果使用了专用用户，登录到 ruc_platform 数据库执行：
-- GRANT ALL ON SCHEMA public TO ruc_admin;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ruc_admin;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO ruc_admin;

-- 4. 验证数据库创建成功
-- \c ruc_platform
-- \dt  -- 此时应无表（Flyway 会在应用首次启动时自动建表）

-- ============================================================
-- 注意：
-- 1. 应用服务器首次启动时，Flyway 会自动执行
--    demo/src/main/resources/db/migration/ 下的所有 SQL 迁移脚本，
--    无需手动导入表结构
-- 2. 建议在数据库服务器上配置防火墙，仅允许应用服务器 IP 访问
--    例如（PostgreSQL 默认端口 5432）：
--    sudo ufw allow from <应用服务器IP> to any port 5432
-- ============================================================
