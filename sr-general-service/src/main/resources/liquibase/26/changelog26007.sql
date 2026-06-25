alter table public.regions
    add column if not exists center_lat numeric(10, 8);

alter table public.regions
    add column if not exists center_lng numeric(11, 8);

alter table public.reports
    add column if not exists region_id bigint;

create index if not exists idx_reports_region_id on public.reports(region_id);

alter table public.reports
    drop constraint if exists reports_region_id_fkey;

alter table public.reports
    add constraint reports_region_id_fkey
        foreign key (region_id) references public.regions(id) on delete set null;

alter table public.dangerous_zones
    add column if not exists region_id bigint;

create index if not exists idx_dangerous_zones_region_id on public.dangerous_zones(region_id);

alter table public.dangerous_zones
    drop constraint if exists dangerous_zones_region_id_fkey;

alter table public.dangerous_zones
    add constraint dangerous_zones_region_id_fkey
        foreign key (region_id) references public.regions(id) on delete set null;