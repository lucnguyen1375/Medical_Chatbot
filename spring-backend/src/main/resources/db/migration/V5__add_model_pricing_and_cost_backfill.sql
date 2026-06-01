create table if not exists model_pricing (
    id uuid primary key default gen_random_uuid(),
    provider varchar(50) not null,
    model varchar(100) not null,
    input_price_per_1m_tokens numeric(12, 6) not null,
    output_price_per_1m_tokens numeric(12, 6) not null,
    currency varchar(3) not null default 'USD',
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint model_pricing_input_price_non_negative check (input_price_per_1m_tokens >= 0),
    constraint model_pricing_output_price_non_negative check (output_price_per_1m_tokens >= 0),
    constraint model_pricing_currency_upper check (currency = upper(currency))
);

create unique index if not exists idx_model_pricing_active_provider_model
    on model_pricing(lower(provider), lower(model))
    where active;

insert into model_pricing (
    id,
    provider,
    model,
    input_price_per_1m_tokens,
    output_price_per_1m_tokens,
    currency,
    active
)
values
    (
        '00000000-0000-0000-0000-000000000701',
        'openai',
        'gpt-4.1-mini',
        0.400000,
        1.600000,
        'USD',
        true
    ),
    (
        '00000000-0000-0000-0000-000000000702',
        'openai',
        'gpt-4o-mini',
        0.150000,
        0.600000,
        'USD',
        true
    )
on conflict (id) do update set
    provider = excluded.provider,
    model = excluded.model,
    input_price_per_1m_tokens = excluded.input_price_per_1m_tokens,
    output_price_per_1m_tokens = excluded.output_price_per_1m_tokens,
    currency = excluded.currency,
    active = excluded.active,
    updated_at = now();

update usage_logs usage
set estimated_cost_usd = round(
        (
            (usage.input_tokens::numeric * pricing.input_price_per_1m_tokens)
            + (usage.output_tokens::numeric * pricing.output_price_per_1m_tokens)
        ) / 1000000,
        6
    )
from model_pricing pricing
where usage.estimated_cost_usd = 0
  and usage.llm_provider is not null
  and usage.llm_model is not null
  and lower(pricing.provider) = lower(usage.llm_provider)
  and lower(pricing.model) = lower(usage.llm_model)
  and pricing.active = true;
