create extension if not exists pgcrypto;

insert into quota_policies (
    id,
    name,
    daily_request_limit,
    daily_token_limit,
    daily_cost_limit_usd
)
values (
    '00000000-0000-0000-0000-000000000101',
    'free_demo',
    50,
    100000,
    1.0000
)
on conflict (name) do update set
    daily_request_limit = excluded.daily_request_limit,
    daily_token_limit = excluded.daily_token_limit,
    daily_cost_limit_usd = excluded.daily_cost_limit_usd;

insert into app_users (
    id,
    username,
    email,
    role,
    quota_policy_id
)
values (
    '00000000-0000-0000-0000-000000000201',
    'demo_user',
    'demo_user@medical-chatbot.local',
    'USER',
    '00000000-0000-0000-0000-000000000101'
)
on conflict (username) do update set
    email = excluded.email,
    role = excluded.role,
    quota_policy_id = excluded.quota_policy_id,
    updated_at = now();
