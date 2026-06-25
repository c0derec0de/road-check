alter table if exists public.reports
    alter column user_id drop not null;

alter table if exists public.reports
    drop constraint if exists reports_user_id_fkey;

alter table if exists public.reports
    add constraint reports_user_id_fkey
        foreign key (user_id)
            references public.users (id)
            on delete set null;
