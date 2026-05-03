-- Create admin user if not exists
CREATE USER admin WITH PASSWORD 'adminpass' CREATEDB;

-- Create database if not exists
CREATE DATABASE agenticdb OWNER admin;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE agenticdb TO admin;
