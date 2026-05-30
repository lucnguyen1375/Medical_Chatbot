create table if not exists quota_policies (
    id uuid primary key default gen_random_uuid(),
    name varchar(100) not null unique,
    daily_request_limit integer not null,
    daily_token_limit integer not null,
    daily_cost_limit_usd numeric(10, 4) not null,
    created_at timestamptz not null default now()
);

create table if not exists app_users (
    id uuid primary key default gen_random_uuid(),
    username varchar(100) not null unique,
    email varchar(255) unique,
    role varchar(50) not null default 'USER',
    quota_policy_id uuid references quota_policies(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists chat_sessions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references app_users(id) on delete cascade,
    title varchar(255),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists chat_messages (
    id uuid primary key default gen_random_uuid(),
    session_id uuid not null references chat_sessions(id) on delete cascade,
    role varchar(30) not null,
    content text not null,
    created_at timestamptz not null default now(),
    constraint chat_messages_role_check check (role in ('user', 'assistant', 'system'))
);

create table if not exists usage_logs (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references app_users(id) on delete set null,
    session_id uuid references chat_sessions(id) on delete set null,
    request_count integer not null default 1,
    input_tokens integer not null default 0,
    output_tokens integer not null default 0,
    estimated_cost_usd numeric(12, 6) not null default 0,
    created_at timestamptz not null default now()
);

create table if not exists cache_entries (
    id uuid primary key default gen_random_uuid(),
    cache_key varchar(255) not null unique,
    value_json jsonb not null,
    expires_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_app_users_quota_policy_id on app_users(quota_policy_id);
create index if not exists idx_chat_sessions_user_id on chat_sessions(user_id);
create index if not exists idx_chat_messages_session_id_created_at on chat_messages(session_id, created_at);
create index if not exists idx_usage_logs_user_id_created_at on usage_logs(user_id, created_at);
create index if not exists idx_usage_logs_session_id_created_at on usage_logs(session_id, created_at);
create index if not exists idx_cache_entries_expires_at on cache_entries(expires_at);
