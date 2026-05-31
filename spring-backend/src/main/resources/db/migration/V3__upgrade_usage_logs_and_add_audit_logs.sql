alter table usage_logs
    add column if not exists llm_provider varchar(50),
    add column if not exists llm_model varchar(100),
    add column if not exists operation varchar(50) not null default 'chat',
    add column if not exists status varchar(30) not null default 'success',
    add column if not exists latency_ms integer,
    add column if not exists error_message text;

alter table usage_logs
    add constraint usage_logs_status_check
    check (status in ('success', 'failed', 'fallback', 'blocked'));

alter table usage_logs
    add constraint usage_logs_latency_ms_check
    check (latency_ms is null or latency_ms >= 0);

create index if not exists idx_usage_logs_model_created_at
    on usage_logs(llm_provider, llm_model, created_at);

create index if not exists idx_usage_logs_status_created_at
    on usage_logs(status, created_at);

create table if not exists audit_logs (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references app_users(id) on delete set null,
    session_id uuid references chat_sessions(id) on delete set null,
    action varchar(100) not null,
    resource_type varchar(100),
    resource_id varchar(100),
    metadata_json jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    constraint audit_logs_action_not_blank check (length(trim(action)) > 0)
);

create index if not exists idx_audit_logs_user_id_created_at
    on audit_logs(user_id, created_at);

create index if not exists idx_audit_logs_session_id_created_at
    on audit_logs(session_id, created_at);

create index if not exists idx_audit_logs_action_created_at
    on audit_logs(action, created_at);
