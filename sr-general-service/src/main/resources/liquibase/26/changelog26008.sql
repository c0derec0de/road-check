alter table public.users add column if not exists role varchar(20);
update public.users set role = 'MODERATOR' where role is null;
alter table public.users alter column role set not null;
alter table public.users alter column role set default 'USER';
