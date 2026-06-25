alter table public.users add column if not exists wallet_address varchar(255);
alter table public.users add column if not exists blockchain_tx_hash varchar(255);
alter table public.users add column if not exists blockchain_verified boolean default false;

alter table public.reports add column if not exists blockchain_verified boolean default false;
alter table public.reports add column if not exists blockchain_block_number bigint;
