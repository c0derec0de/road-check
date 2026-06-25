update public.reports set status = 'NEW' where status = 'new';
update public.reports set status = 'IN_PROGRESS' where status = 'in_progress';
update public.reports set status = 'COMPLETED' where status = 'completed';

alter table public.reports alter column status set default 'NEW';
