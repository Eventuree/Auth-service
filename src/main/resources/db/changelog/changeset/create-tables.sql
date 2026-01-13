-- liquibase formatted sql

--changeset bovsunovsky:create-tables

CREATE TABLE IF NOT EXISTS refresh_tokens(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    token_hash VARCHAR(255),
    expires_at TIMESTAMP,
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    client_ip VARCHAR(50),
    user_agent VARCHAR
);

CREATE TABLE IF NOT EXISTS auth_credentials(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    email VARCHAR(255),
    password_hash VARCHAR(255),
    account_status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS social_auth_identities(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    provider_name VARCHAR(50),
    provider_user_id VARCHAR(255),
    login_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) DEFAULT 'USER',
    auth_provider VARCHAR(50) DEFAULT 'LOCAL'
);