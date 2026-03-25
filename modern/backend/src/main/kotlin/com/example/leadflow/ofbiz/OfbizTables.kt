package com.example.leadflow.ofbiz

import org.jooq.Field
import org.jooq.Table
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import java.math.BigDecimal
import java.time.OffsetDateTime

private fun table(name: String): Table<*> = DSL.table(DSL.name(name))

private fun varchar(name: String): Field<String> = DSL.field(DSL.name(name), SQLDataType.VARCHAR)

private fun char(name: String): Field<String> = DSL.field(DSL.name(name), SQLDataType.CHAR)

private fun numeric(name: String): Field<BigDecimal> = DSL.field(DSL.name(name), SQLDataType.NUMERIC)

private fun timestamp(name: String): Field<OffsetDateTime> = DSL.field(DSL.name(name), SQLDataType.TIMESTAMPWITHTIMEZONE)

object OfbizTables {
    object SequenceValueItem {
        val TABLE = table("sequence_value_item")
        val SEQ_NAME = varchar("seq_name")
        val SEQ_ID = numeric("seq_id")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object Party {
        val TABLE = table("party")
        val PARTY_ID = varchar("party_id")
        val PARTY_TYPE_ID = varchar("party_type_id")
        val STATUS_ID = varchar("status_id")
        val DESCRIPTION = varchar("description")
        val CREATED_DATE = timestamp("created_date")
        val CREATED_BY_USER_LOGIN = varchar("created_by_user_login")
        val LAST_MODIFIED_DATE = timestamp("last_modified_date")
        val LAST_MODIFIED_BY_USER_LOGIN = varchar("last_modified_by_user_login")
        val DATA_SOURCE_ID = varchar("data_source_id")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object Person {
        val TABLE = table("person")
        val PARTY_ID = varchar("party_id")
        val FIRST_NAME = varchar("first_name")
        val LAST_NAME = varchar("last_name")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object PartyGroup {
        val TABLE = table("party_group")
        val PARTY_ID = varchar("party_id")
        val GROUP_NAME = varchar("group_name")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object PartyRole {
        val TABLE = table("party_role")
        val PARTY_ID = varchar("party_id")
        val ROLE_TYPE_ID = varchar("role_type_id")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object PartyRelationship {
        val TABLE = table("party_relationship")
        val PARTY_ID_FROM = varchar("party_id_from")
        val PARTY_ID_TO = varchar("party_id_to")
        val ROLE_TYPE_ID_FROM = varchar("role_type_id_from")
        val ROLE_TYPE_ID_TO = varchar("role_type_id_to")
        val FROM_DATE = timestamp("from_date")
        val THRU_DATE = timestamp("thru_date")
        val STATUS_ID = varchar("status_id")
        val PARTY_RELATIONSHIP_TYPE_ID = varchar("party_relationship_type_id")
        val POSITION_TITLE = varchar("position_title")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object ContactMech {
        val TABLE = table("contact_mech")
        val CONTACT_MECH_ID = varchar("contact_mech_id")
        val CONTACT_MECH_TYPE_ID = varchar("contact_mech_type_id")
        val INFO_STRING = varchar("info_string")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object PartyContactMech {
        val TABLE = table("party_contact_mech")
        val PARTY_ID = varchar("party_id")
        val CONTACT_MECH_ID = varchar("contact_mech_id")
        val FROM_DATE = timestamp("from_date")
        val THRU_DATE = timestamp("thru_date")
        val ROLE_TYPE_ID = varchar("role_type_id")
        val VERIFIED = char("verified")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object PartyContactMechPurpose {
        val TABLE = table("party_contact_mech_purpose")
        val PARTY_ID = varchar("party_id")
        val CONTACT_MECH_ID = varchar("contact_mech_id")
        val CONTACT_MECH_PURPOSE_TYPE_ID = varchar("contact_mech_purpose_type_id")
        val FROM_DATE = timestamp("from_date")
        val THRU_DATE = timestamp("thru_date")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object PartyDataSource {
        val TABLE = table("party_data_source")
        val PARTY_ID = varchar("party_id")
        val DATA_SOURCE_ID = varchar("data_source_id")
        val FROM_DATE = timestamp("from_date")
        val IS_CREATE = char("is_create")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object UserLogin {
        val TABLE = table("user_login")
        val USER_LOGIN_ID = varchar("user_login_id")
        val PARTY_ID = varchar("party_id")
    }

    object CustRequest {
        val TABLE = table("cust_request")
        val CUST_REQUEST_ID = varchar("cust_request_id")
        val CUST_REQUEST_TYPE_ID = varchar("cust_request_type_id")
        val STATUS_ID = varchar("status_id")
        val FROM_PARTY_ID = varchar("from_party_id")
        val PRIORITY = numeric("priority")
        val CUST_REQUEST_DATE = timestamp("cust_request_date")
        val CUST_REQUEST_NAME = varchar("cust_request_name")
        val DESCRIPTION = varchar("description")
        val MAXIMUM_AMOUNT_UOM_ID = varchar("maximum_amount_uom_id")
        val PRODUCT_STORE_ID = varchar("product_store_id")
        val SALES_CHANNEL_ENUM_ID = varchar("sales_channel_enum_id")
        val CURRENCY_UOM_ID = varchar("currency_uom_id")
        val CREATED_DATE = timestamp("created_date")
        val CREATED_BY_USER_LOGIN = varchar("created_by_user_login")
        val LAST_MODIFIED_DATE = timestamp("last_modified_date")
        val LAST_MODIFIED_BY_USER_LOGIN = varchar("last_modified_by_user_login")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object CustRequestItem {
        val TABLE = table("cust_request_item")
        val CUST_REQUEST_ID = varchar("cust_request_id")
        val CUST_REQUEST_ITEM_SEQ_ID = varchar("cust_request_item_seq_id")
        val STATUS_ID = varchar("status_id")
        val PRODUCT_ID = varchar("product_id")
        val QUANTITY = numeric("quantity")
        val SELECTED_AMOUNT = numeric("selected_amount")
        val MAXIMUM_AMOUNT = numeric("maximum_amount")
        val DESCRIPTION = varchar("description")
        val STORY = varchar("story")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object CustRequestParty {
        val TABLE = table("cust_request_party")
        val CUST_REQUEST_ID = varchar("cust_request_id")
        val PARTY_ID = varchar("party_id")
        val ROLE_TYPE_ID = varchar("role_type_id")
        val FROM_DATE = timestamp("from_date")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object CustRequestStatus {
        val TABLE = table("cust_request_status")
        val CUST_REQUEST_STATUS_ID = varchar("cust_request_status_id")
        val STATUS_ID = varchar("status_id")
        val CUST_REQUEST_ID = varchar("cust_request_id")
        val STATUS_DATE = timestamp("status_date")
        val CHANGE_BY_USER_LOGIN_ID = varchar("change_by_user_login_id")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object Quote {
        val TABLE = table("quote")
        val QUOTE_ID = varchar("quote_id")
        val QUOTE_TYPE_ID = varchar("quote_type_id")
        val PARTY_ID = varchar("party_id")
        val ISSUE_DATE = timestamp("issue_date")
        val STATUS_ID = varchar("status_id")
        val CURRENCY_UOM_ID = varchar("currency_uom_id")
        val PRODUCT_STORE_ID = varchar("product_store_id")
        val SALES_CHANNEL_ENUM_ID = varchar("sales_channel_enum_id")
        val VALID_FROM_DATE = timestamp("valid_from_date")
        val VALID_THRU_DATE = timestamp("valid_thru_date")
        val QUOTE_NAME = varchar("quote_name")
        val DESCRIPTION = varchar("description")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object QuoteItem {
        val TABLE = table("quote_item")
        val QUOTE_ID = varchar("quote_id")
        val QUOTE_ITEM_SEQ_ID = varchar("quote_item_seq_id")
        val PRODUCT_ID = varchar("product_id")
        val CUST_REQUEST_ID = varchar("cust_request_id")
        val CUST_REQUEST_ITEM_SEQ_ID = varchar("cust_request_item_seq_id")
        val QUANTITY = numeric("quantity")
        val SELECTED_AMOUNT = numeric("selected_amount")
        val QUOTE_UNIT_PRICE = numeric("quote_unit_price")
        val COMMENTS = varchar("comments")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object QuoteRole {
        val TABLE = table("quote_role")
        val QUOTE_ID = varchar("quote_id")
        val PARTY_ID = varchar("party_id")
        val ROLE_TYPE_ID = varchar("role_type_id")
        val FROM_DATE = timestamp("from_date")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object Product {
        val TABLE = table("product")
        val PRODUCT_ID = varchar("product_id")
        val INTERNAL_NAME = varchar("internal_name")
        val PRODUCT_NAME = varchar("product_name")
    }

    object OrderHeader {
        val TABLE = table("order_header")
        val ORDER_ID = varchar("order_id")
        val ORDER_TYPE_ID = varchar("order_type_id")
        val ORDER_NAME = varchar("order_name")
        val SALES_CHANNEL_ENUM_ID = varchar("sales_channel_enum_id")
        val ORDER_DATE = timestamp("order_date")
        val PRIORITY = char("priority")
        val ENTRY_DATE = timestamp("entry_date")
        val STATUS_ID = varchar("status_id")
        val CREATED_BY = varchar("created_by")
        val CURRENCY_UOM = varchar("currency_uom")
        val WEB_SITE_ID = varchar("web_site_id")
        val PRODUCT_STORE_ID = varchar("product_store_id")
        val REMAINING_SUB_TOTAL = numeric("remaining_sub_total")
        val GRAND_TOTAL = numeric("grand_total")
        val INVOICE_PER_SHIPMENT = char("invoice_per_shipment")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object OrderItem {
        val TABLE = table("order_item")
        val ORDER_ID = varchar("order_id")
        val ORDER_ITEM_SEQ_ID = varchar("order_item_seq_id")
        val ORDER_ITEM_TYPE_ID = varchar("order_item_type_id")
        val PRODUCT_ID = varchar("product_id")
        val IS_PROMO = char("is_promo")
        val QUOTE_ID = varchar("quote_id")
        val QUOTE_ITEM_SEQ_ID = varchar("quote_item_seq_id")
        val QUANTITY = numeric("quantity")
        val SELECTED_AMOUNT = numeric("selected_amount")
        val UNIT_PRICE = numeric("unit_price")
        val UNIT_LIST_PRICE = numeric("unit_list_price")
        val IS_MODIFIED_PRICE = char("is_modified_price")
        val ITEM_DESCRIPTION = varchar("item_description")
        val STATUS_ID = varchar("status_id")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object OrderRole {
        val TABLE = table("order_role")
        val ORDER_ID = varchar("order_id")
        val PARTY_ID = varchar("party_id")
        val ROLE_TYPE_ID = varchar("role_type_id")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }

    object OrderStatus {
        val TABLE = table("order_status")
        val ORDER_STATUS_ID = varchar("order_status_id")
        val STATUS_ID = varchar("status_id")
        val ORDER_ID = varchar("order_id")
        val ORDER_ITEM_SEQ_ID = varchar("order_item_seq_id")
        val STATUS_DATETIME = timestamp("status_datetime")
        val STATUS_USER_LOGIN = varchar("status_user_login")
        val LAST_UPDATED_STAMP = timestamp("last_updated_stamp")
        val LAST_UPDATED_TX_STAMP = timestamp("last_updated_tx_stamp")
        val CREATED_STAMP = timestamp("created_stamp")
        val CREATED_TX_STAMP = timestamp("created_tx_stamp")
    }
}
