alter table chat_sessions
    add column if not exists active_patient_id varchar(100),
    add column if not exists memory_summary text,
    add column if not exists last_intent varchar(100),
    add column if not exists last_tool_name varchar(100),
    add column if not exists last_resource_type varchar(100),
    add column if not exists last_resource_id varchar(100);

alter table chat_messages
    add column if not exists metadata_json jsonb not null default '{}'::jsonb;

create index if not exists idx_chat_sessions_user_active_patient
    on chat_sessions(user_id, active_patient_id);
