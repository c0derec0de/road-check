alter table public.users add column if not exists login varchar(255);

update public.users
set login = coalesce(nullif(login, ''), nullif(vk_id, ''), nullif(email, ''), nullif(phone, ''), 'user_' || id::text)
where login is null or login = '';

alter table public.users alter column login set not null;
