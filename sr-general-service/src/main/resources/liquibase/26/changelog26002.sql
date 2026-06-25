create table if not exists public.risk_predictions (
    id bigserial primary key,
    risk_score numeric(5, 2),
    latitude numeric(10, 8),
    longitude numeric(11, 8),
    calculated_at timestamp default now(),
    created_at timestamp default now() not null,
    updated_at timestamp default now() not null
);

alter table public.reports add column if not exists risk_level varchar(10);
alter table public.reports add column if not exists title varchar(255);
alter table public.reports add column if not exists address varchar(500);
alter table public.reports add column if not exists is_dangerous_zone boolean default false;
