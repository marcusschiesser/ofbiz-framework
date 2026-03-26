create table sequence_value_item (
  seq_name varchar(100) primary key,
  seq_id decimal(18, 0) not null,
  last_updated_stamp timestamp,
  last_updated_tx_stamp timestamp,
  created_stamp timestamp,
  created_tx_stamp timestamp
);

create table party (
  party_id varchar(60) primary key,
  party_type_id varchar(60) not null,
  status_id varchar(60),
  created_date timestamp,
  created_by_user_login varchar(60),
  last_modified_date timestamp,
  last_modified_by_user_login varchar(60),
  data_source_id varchar(60),
  last_updated_stamp timestamp,
  last_updated_tx_stamp timestamp,
  created_stamp timestamp,
  created_tx_stamp timestamp
);

create table person (
  party_id varchar(60) primary key,
  first_name varchar(255),
  last_name varchar(255),
  last_updated_stamp timestamp,
  last_updated_tx_stamp timestamp,
  created_stamp timestamp,
  created_tx_stamp timestamp
);

create table party_group (
  party_id varchar(60) primary key,
  group_name varchar(255),
  last_updated_stamp timestamp,
  last_updated_tx_stamp timestamp,
  created_stamp timestamp,
  created_tx_stamp timestamp
);

create table party_role (
  party_id varchar(60) not null,
  role_type_id varchar(60) not null,
  last_updated_stamp timestamp,
  last_updated_tx_stamp timestamp,
  created_stamp timestamp,
  created_tx_stamp timestamp,
  primary key (party_id, role_type_id)
);

create table party_relationship (
  party_id_from varchar(60) not null,
  party_id_to varchar(60) not null,
  role_type_id_from varchar(60) not null,
  role_type_id_to varchar(60) not null,
  from_date timestamp not null,
  thru_date timestamp,
  status_id varchar(60),
  relationship_name varchar(255),
  security_group_id varchar(60),
  priority_type_id varchar(60),
  party_relationship_type_id varchar(60),
  permissions_enum_id varchar(60),
  position_title varchar(255),
  comments varchar(2000),
  last_updated_stamp timestamp,
  last_updated_tx_stamp timestamp,
  created_stamp timestamp,
  created_tx_stamp timestamp,
  primary key (party_id_from, party_id_to, role_type_id_from, role_type_id_to, from_date)
);

create table party_status (
  status_id varchar(60) not null,
  party_id varchar(60) not null,
  status_date timestamp not null,
  change_by_user_login_id varchar(60),
  last_updated_stamp timestamp,
  last_updated_tx_stamp timestamp,
  created_stamp timestamp,
  created_tx_stamp timestamp,
  primary key (status_id, party_id, status_date)
);

create table contact_mech (
  contact_mech_id varchar(60) primary key,
  contact_mech_type_id varchar(60) not null,
  info_string varchar(2000),
  last_updated_stamp timestamp,
  last_updated_tx_stamp timestamp,
  created_stamp timestamp,
  created_tx_stamp timestamp
);

create table party_contact_mech (
  party_id varchar(60) not null,
  contact_mech_id varchar(60) not null,
  from_date timestamp not null,
  thru_date timestamp,
  role_type_id varchar(60),
  allow_solicitation varchar(1),
  extension varchar(255),
  verified varchar(1),
  comments varchar(2000),
  years_with_contact_mech decimal(18, 0),
  months_with_contact_mech decimal(18, 0),
  last_updated_stamp timestamp,
  last_updated_tx_stamp timestamp,
  created_stamp timestamp,
  created_tx_stamp timestamp,
  primary key (party_id, contact_mech_id, from_date)
);

create table party_contact_mech_purpose (
  party_id varchar(60) not null,
  contact_mech_id varchar(60) not null,
  contact_mech_purpose_type_id varchar(60) not null,
  from_date timestamp not null,
  thru_date timestamp,
  last_updated_stamp timestamp,
  last_updated_tx_stamp timestamp,
  created_stamp timestamp,
  created_tx_stamp timestamp,
  primary key (party_id, contact_mech_id, contact_mech_purpose_type_id, from_date)
);

create table party_data_source (
  party_id varchar(60) not null,
  data_source_id varchar(60) not null,
  from_date timestamp not null,
  visit_id varchar(60),
  comments varchar(2000),
  is_create varchar(1),
  last_updated_stamp timestamp,
  last_updated_tx_stamp timestamp,
  created_stamp timestamp,
  created_tx_stamp timestamp,
  primary key (party_id, data_source_id, from_date)
);

create table cust_request (
  cust_request_id varchar(60) primary key,
  cust_request_type_id varchar(60),
  status_id varchar(60),
  from_party_id varchar(60),
  priority decimal(18, 2),
  cust_request_date timestamp,
  cust_request_name varchar(255),
  description varchar(2000),
  maximum_amount_uom_id varchar(60),
  product_store_id varchar(60),
  sales_channel_enum_id varchar(60),
  fulfill_contact_mech_id varchar(60),
  currency_uom_id varchar(60),
  open_date_time timestamp,
  closed_date_time timestamp,
  internal_comment varchar(2000),
  reason varchar(2000),
  created_date timestamp,
  created_by_user_login varchar(60),
  last_modified_date timestamp,
  last_modified_by_user_login varchar(60),
  last_updated_stamp timestamp,
  last_updated_tx_stamp timestamp,
  created_stamp timestamp,
  created_tx_stamp timestamp
);

create table cust_request_item (
  cust_request_id varchar(60) not null,
  cust_request_item_seq_id varchar(60) not null,
  cust_request_resolution_id varchar(60),
  status_id varchar(60),
  priority decimal(18, 2),
  sequence_num decimal(18, 2),
  required_by_date timestamp,
  product_id varchar(60),
  quantity decimal(18, 2),
  selected_amount decimal(18, 2),
  maximum_amount decimal(18, 2),
  reserv_start timestamp,
  reserv_length decimal(18, 2),
  reserv_persons decimal(18, 2),
  config_id varchar(60),
  description varchar(2000),
  story varchar(4000),
  last_updated_stamp timestamp,
  last_updated_tx_stamp timestamp,
  created_stamp timestamp,
  created_tx_stamp timestamp,
  primary key (cust_request_id, cust_request_item_seq_id)
);

create table cust_request_party (
  cust_request_id varchar(60) not null,
  party_id varchar(60) not null,
  role_type_id varchar(60) not null,
  from_date timestamp not null,
  thru_date timestamp,
  last_updated_stamp timestamp,
  last_updated_tx_stamp timestamp,
  created_stamp timestamp,
  created_tx_stamp timestamp,
  primary key (cust_request_id, party_id, role_type_id, from_date)
);

create table cust_request_status (
  cust_request_status_id varchar(60) primary key,
  status_id varchar(60),
  cust_request_id varchar(60),
  cust_request_item_seq_id varchar(60),
  status_date timestamp,
  change_by_user_login_id varchar(60),
  last_updated_stamp timestamp,
  last_updated_tx_stamp timestamp,
  created_stamp timestamp,
  created_tx_stamp timestamp
);

create table quote (
  quote_id varchar(60) primary key,
  quote_type_id varchar(60),
  party_id varchar(60),
  issue_date timestamp,
  status_id varchar(60),
  currency_uom_id varchar(60),
  product_store_id varchar(60),
  sales_channel_enum_id varchar(60),
  valid_from_date timestamp,
  valid_thru_date timestamp,
  quote_name varchar(255),
  description varchar(2000)
);

create table quote_item (
  quote_id varchar(60) not null,
  quote_item_seq_id varchar(60) not null,
  product_id varchar(60),
  cust_request_id varchar(60),
  cust_request_item_seq_id varchar(60),
  quantity decimal(18, 2),
  selected_amount decimal(18, 2),
  quote_unit_price decimal(18, 2),
  comments varchar(2000),
  primary key (quote_id, quote_item_seq_id)
);
