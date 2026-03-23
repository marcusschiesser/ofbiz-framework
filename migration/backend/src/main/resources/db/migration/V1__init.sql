create sequence if not exists order_number_seq start with 1000 increment by 1;

create table if not exists customers (
    id uuid primary key,
    customer_number varchar(32) not null unique,
    name varchar(200) not null,
    email varchar(200) not null unique,
    created_at timestamptz not null default current_timestamp
);

create table if not exists products (
    id uuid primary key,
    sku varchar(64) not null unique,
    name varchar(200) not null,
    unit_price numeric(12, 2) not null,
    currency_code varchar(3) not null,
    active boolean not null default true,
    created_at timestamptz not null default current_timestamp
);

create table if not exists orders (
    id uuid primary key,
    order_number varchar(32) not null unique,
    customer_id uuid not null references customers(id),
    status varchar(32) not null check (status in ('PENDING', 'PAID', 'SHIPPED', 'COMPLETED', 'CANCELLED')),
    notes text,
    currency_code varchar(3) not null,
    subtotal numeric(12, 2) not null check (subtotal >= 0),
    paid_total numeric(12, 2) not null default 0 check (paid_total >= 0),
    refunded_total numeric(12, 2) not null default 0 check (refunded_total >= 0),
    tracking_number varchar(120),
    payment_reference varchar(120),
    paid_at timestamptz,
    shipped_at timestamptz,
    completed_at timestamptz,
    cancelled_at timestamptz,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0
);

create index if not exists idx_orders_status on orders(status);
create index if not exists idx_orders_customer on orders(customer_id);

create table if not exists order_items (
    id uuid primary key,
    order_id uuid not null references orders(id) on delete cascade,
    product_id uuid references products(id),
    line_number integer not null check (line_number > 0),
    sku varchar(64) not null,
    product_name varchar(200) not null,
    quantity integer not null check (quantity > 0),
    unit_price numeric(12, 2) not null check (unit_price >= 0),
    line_total numeric(12, 2) not null check (line_total >= 0)
);

create unique index if not exists idx_order_items_order_line on order_items(order_id, line_number);

create table if not exists order_events (
    id uuid primary key,
    order_id uuid not null references orders(id) on delete cascade,
    event_type varchar(32) not null,
    status_after varchar(32),
    actor varchar(120) not null,
    reason text,
    payload jsonb not null default '{}'::jsonb,
    occurred_at timestamptz not null
);

create index if not exists idx_order_events_order_id on order_events(order_id, occurred_at desc);

create table if not exists order_refunds (
    id uuid primary key,
    order_id uuid not null references orders(id) on delete cascade,
    amount numeric(12, 2) not null check (amount > 0),
    actor varchar(120) not null,
    reason text not null,
    created_at timestamptz not null
);

create index if not exists idx_order_refunds_order_id on order_refunds(order_id, created_at desc);
