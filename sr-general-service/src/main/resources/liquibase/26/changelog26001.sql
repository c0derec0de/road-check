create table if not exists public.users (
    id bigserial primary key,
    login varchar(255) not null,
    vk_id varchar(255),
    firstname varchar(100),
    middlename varchar(100),
    lastname varchar(100),
    department varchar(150),
    city varchar(100),
    phone varchar(25),
    email varchar(255),
    created_at timestamp default now() not null,
    updated_at timestamp default now() not null
);

create table if not exists public.dangerous_zones (
    id bigserial primary key,
    name text,
    center_lat numeric(9, 6),
    center_lng numeric(9, 6),
    radius integer,
    incidents_count integer,
    risk_level varchar(10),
    calculated_at timestamp default now(),
    is_active boolean default true,
    created_at timestamp default now() not null,
    updated_at timestamp default now() not null
);

create table if not exists public.reports (
    id bigserial primary key,
    police_user_id bigint not null,
    user_id bigint not null,
    incident_type varchar(100) not null,
    latitude numeric(10, 8),
    longitude numeric(11, 8),
    description text,
    comment text,
    photos_uuid varchar(255),
    status varchar(50) default 'new',
    blockchain_tx_hash varchar(255),
    fatalities integer,
    injuries integer,
    cause varchar(255),
    created_at timestamp default now() not null,
    updated_at timestamp default now() not null,
    constraint reports_police_user_id_fkey foreign key (police_user_id) references public.users(id),
    constraint reports_user_id_fkey foreign key (user_id) references public.users(id)
);

create table if not exists public.weather (
    id bigserial primary key,
    timestamp timestamp default current_timestamp not null,
    humidity numeric(5, 2),
    temperature numeric(5, 2),
    wind_direction varchar(50),
    wind_speed numeric(6, 2),
    cloud_cover numeric(5, 2),
    visibility numeric(8, 2),
    dew_point numeric(5, 2),
    precipitation numeric(6, 2),
    current_weather varchar(100),
    past_weather_1 varchar(100),
    past_weather_2 varchar(100),
    cloud_height numeric(8, 2),
    latitude numeric(10, 8),
    longitude numeric(11, 8),
    created_at timestamp default now() not null,
    updated_at timestamp default now() not null
);

create table if not exists public.regions (
    id bigserial primary key,
    reg_code varchar(20) not null,
    reg_name varchar(100) not null,
    created_at timestamp default now() not null,
    updated_at timestamp default now() not null
);

create table if not exists public.roads (
    id bigserial primary key,
    road_name varchar(255),
    created_at timestamp default now() not null,
    updated_at timestamp default now() not null
);

create table if not exists public.regions_roads (
    region_id bigint not null,
    road_id bigint not null,
    constraint regions_roads_pkey primary key (region_id, road_id),
    constraint regions_roads_region_id_fkey foreign key (region_id) references public.regions(id),
    constraint regions_roads_road_id_fkey foreign key (road_id) references public.roads(id)
);
