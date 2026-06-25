alter table public.risk_predictions add column if not exists zone_id bigint;
alter table public.risk_predictions add column if not exists region_name varchar(100);
alter table public.risk_predictions add column if not exists city_name varchar(100);
alter table public.risk_predictions add column if not exists district_name varchar(100);
alter table public.risk_predictions add column if not exists risk_level varchar(10);
alter table public.risk_predictions add column if not exists probability_high numeric(5, 4);
alter table public.risk_predictions add column if not exists probability_medium numeric(5, 4);
alter table public.risk_predictions add column if not exists probability_low numeric(5, 4);
alter table public.risk_predictions add column if not exists predicted_incidents_next_7d integer;
alter table public.risk_predictions add column if not exists predicted_incidents_next_30d integer;
alter table public.risk_predictions add column if not exists confidence_level numeric(5, 4);
alter table public.risk_predictions add column if not exists model_version varchar(50);
alter table public.risk_predictions add column if not exists model_accuracy numeric(5, 4);
alter table public.risk_predictions add column if not exists forecast_date date;
alter table public.risk_predictions add column if not exists forecast_end_date date;
alter table public.risk_predictions add column if not exists is_active boolean default true;
alter table public.risk_predictions add column if not exists contributing_factors jsonb;
alter table public.risk_predictions add column if not exists weather_contribution numeric(5, 4);
alter table public.risk_predictions add column if not exists historical_contribution numeric(5, 4);
alter table public.risk_predictions add column if not exists seasonal_contribution numeric(5, 4);
alter table public.risk_predictions add column if not exists validation_method varchar(50);
alter table public.risk_predictions add column if not exists validation_score numeric(5, 4);

update public.risk_predictions set risk_level = 'medium' where risk_level is null;
update public.risk_predictions set model_version = 'v1' where model_version is null;

alter table public.risk_predictions alter column risk_level set not null;
alter table public.risk_predictions alter column model_version set not null;

alter table public.risk_predictions drop constraint if exists risk_predictions_risk_level_check;
alter table public.risk_predictions add constraint risk_predictions_risk_level_check
    check (risk_level in ('high', 'medium', 'low'));

alter table public.risk_predictions drop constraint if exists risk_predictions_risk_score_check;
alter table public.risk_predictions add constraint risk_predictions_risk_score_check
    check (risk_score >= 0 and risk_score <= 100);

alter table public.risk_predictions drop constraint if exists risk_predictions_probability_check;
alter table public.risk_predictions add constraint risk_predictions_probability_check
    check (
        probability_high >= 0 and probability_high <= 1 and
        probability_medium >= 0 and probability_medium <= 1 and
        probability_low >= 0 and probability_low <= 1
        );

alter table public.risk_predictions drop constraint if exists risk_predictions_zone_id_fkey;
alter table public.risk_predictions add constraint risk_predictions_zone_id_fkey
    foreign key (zone_id) references public.dangerous_zones(id) on delete set null;