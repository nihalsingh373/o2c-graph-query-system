-- ================================================================
-- SAP O2C Graph System - Full PostgreSQL Schema
-- Run this once in Supabase SQL Editor before starting the app
-- ================================================================

-- ========================
-- RAW DATA TABLES
-- ========================

CREATE TABLE IF NOT EXISTS sales_order_headers (
    sales_order VARCHAR(20) PRIMARY KEY,
    sales_order_type VARCHAR(10),
    sales_organization VARCHAR(10),
    distribution_channel VARCHAR(5),
    organization_division VARCHAR(5),
    sales_group VARCHAR(10),
    sales_office VARCHAR(10),
    sold_to_party VARCHAR(20),
    creation_date TIMESTAMP,
    created_by_user VARCHAR(20),
    last_change_date_time TIMESTAMP,
    total_net_amount NUMERIC(18,2),
    overall_delivery_status VARCHAR(5),
    overall_ord_reltd_billg_status VARCHAR(5),
    overall_sd_doc_reference_status VARCHAR(5),
    transaction_currency VARCHAR(5),
    pricing_date TIMESTAMP,
    requested_delivery_date TIMESTAMP,
    header_billing_block_reason VARCHAR(10),
    delivery_block_reason VARCHAR(10),
    incoterms_classification VARCHAR(10),
    incoterms_location1 VARCHAR(100),
    customer_payment_terms VARCHAR(10),
    total_credit_check_status VARCHAR(5)
);

CREATE TABLE IF NOT EXISTS sales_order_items (
    sales_order VARCHAR(20),
    sales_order_item VARCHAR(10),
    sales_order_item_category VARCHAR(10),
    material VARCHAR(30),
    requested_quantity NUMERIC(18,3),
    requested_quantity_unit VARCHAR(5),
    transaction_currency VARCHAR(5),
    net_amount NUMERIC(18,2),
    material_group VARCHAR(10),
    production_plant VARCHAR(10),
    storage_location VARCHAR(10),
    sales_document_rjcn_reason VARCHAR(5),
    item_billing_block_reason VARCHAR(5),
    PRIMARY KEY (sales_order, sales_order_item)
);

CREATE TABLE IF NOT EXISTS sales_order_schedule_lines (
    sales_order VARCHAR(20),
    sales_order_item VARCHAR(10),
    schedule_line VARCHAR(10),
    confirmed_delivery_date TIMESTAMP,
    order_quantity_unit VARCHAR(5),
    confd_order_qty_by_matl_avail_check NUMERIC(18,3),
    PRIMARY KEY (sales_order, sales_order_item, schedule_line)
);

CREATE TABLE IF NOT EXISTS outbound_delivery_headers (
    delivery_document VARCHAR(20) PRIMARY KEY,
    actual_goods_movement_date TIMESTAMP,
    actual_goods_movement_time VARCHAR(10),
    creation_date TIMESTAMP,
    creation_time VARCHAR(10),
    delivery_block_reason VARCHAR(5),
    hdr_general_incompletion_status VARCHAR(5),
    header_billing_block_reason VARCHAR(5),
    last_change_date TIMESTAMP,
    overall_goods_movement_status VARCHAR(5),
    overall_picking_status VARCHAR(5),
    overall_proof_of_delivery_status VARCHAR(5),
    shipping_point VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS outbound_delivery_items (
    delivery_document VARCHAR(20),
    delivery_document_item VARCHAR(10),
    actual_delivery_quantity NUMERIC(18,3),
    batch VARCHAR(20),
    delivery_quantity_unit VARCHAR(5),
    item_billing_block_reason VARCHAR(5),
    last_change_date TIMESTAMP,
    plant VARCHAR(10),
    reference_sd_document VARCHAR(20),
    reference_sd_document_item VARCHAR(10),
    storage_location VARCHAR(10),
    PRIMARY KEY (delivery_document, delivery_document_item)
);

CREATE TABLE IF NOT EXISTS billing_document_headers (
    billing_document VARCHAR(20) PRIMARY KEY,
    billing_document_type VARCHAR(10),
    creation_date TIMESTAMP,
    creation_time VARCHAR(10),
    last_change_date_time TIMESTAMP,
    billing_document_date TIMESTAMP,
    billing_document_is_cancelled VARCHAR(5),
    cancelled_billing_document VARCHAR(20),
    total_net_amount NUMERIC(18,2),
    transaction_currency VARCHAR(5),
    company_code VARCHAR(10),
    fiscal_year VARCHAR(5),
    accounting_document VARCHAR(20),
    sold_to_party VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS billing_document_items (
    billing_document VARCHAR(20),
    billing_document_item VARCHAR(10),
    material VARCHAR(30),
    billing_quantity NUMERIC(18,3),
    billing_quantity_unit VARCHAR(5),
    net_amount NUMERIC(18,2),
    transaction_currency VARCHAR(5),
    reference_sd_document VARCHAR(20),
    reference_sd_document_item VARCHAR(10),
    PRIMARY KEY (billing_document, billing_document_item)
);

CREATE TABLE IF NOT EXISTS billing_document_cancellations (
    billing_document VARCHAR(20) PRIMARY KEY,
    billing_document_type VARCHAR(10),
    creation_date TIMESTAMP,
    creation_time VARCHAR(10),
    last_change_date_time TIMESTAMP,
    billing_document_date TIMESTAMP,
    billing_document_is_cancelled VARCHAR(5),
    cancelled_billing_document VARCHAR(20),
    total_net_amount NUMERIC(18,2),
    transaction_currency VARCHAR(5),
    company_code VARCHAR(10),
    fiscal_year VARCHAR(5),
    accounting_document VARCHAR(20),
    sold_to_party VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS journal_entry_items_ar (
    company_code VARCHAR(10),
    fiscal_year VARCHAR(5),
    accounting_document VARCHAR(20),
    accounting_document_item VARCHAR(10),
    gl_account VARCHAR(20),
    reference_document VARCHAR(20),
    cost_center VARCHAR(20),
    profit_center VARCHAR(20),
    transaction_currency VARCHAR(5),
    amount_in_transaction_currency NUMERIC(18,2),
    company_code_currency VARCHAR(5),
    amount_in_company_code_currency NUMERIC(18,2),
    posting_date TIMESTAMP,
    document_date TIMESTAMP,
    accounting_document_type VARCHAR(5),
    assignment_reference VARCHAR(50),
    last_change_date_time TIMESTAMP,
    customer VARCHAR(20),
    financial_account_type VARCHAR(5),
    clearing_date TIMESTAMP,
    clearing_accounting_document VARCHAR(20),
    clearing_doc_fiscal_year VARCHAR(5),
    PRIMARY KEY (company_code, fiscal_year, accounting_document, accounting_document_item)
);

CREATE TABLE IF NOT EXISTS payments_ar (
    company_code VARCHAR(10),
    fiscal_year VARCHAR(5),
    accounting_document VARCHAR(20),
    accounting_document_item VARCHAR(10),
    clearing_date TIMESTAMP,
    clearing_accounting_document VARCHAR(20),
    clearing_doc_fiscal_year VARCHAR(5),
    amount_in_transaction_currency NUMERIC(18,2),
    transaction_currency VARCHAR(5),
    amount_in_company_code_currency NUMERIC(18,2),
    company_code_currency VARCHAR(5),
    customer VARCHAR(20),
    invoice_reference VARCHAR(20),
    invoice_reference_fiscal_year VARCHAR(5),
    sales_document VARCHAR(20),
    sales_document_item VARCHAR(10),
    posting_date TIMESTAMP,
    document_date TIMESTAMP,
    assignment_reference VARCHAR(50),
    gl_account VARCHAR(20),
    financial_account_type VARCHAR(5),
    profit_center VARCHAR(20),
    cost_center VARCHAR(20),
    PRIMARY KEY (company_code, fiscal_year, accounting_document, accounting_document_item)
);

CREATE TABLE IF NOT EXISTS business_partners (
    business_partner VARCHAR(20) PRIMARY KEY,
    customer VARCHAR(20),
    business_partner_category VARCHAR(5),
    business_partner_full_name VARCHAR(200),
    business_partner_grouping VARCHAR(10),
    business_partner_name VARCHAR(200),
    correspondence_language VARCHAR(5),
    created_by_user VARCHAR(20),
    creation_date TIMESTAMP,
    creation_time VARCHAR(10),
    first_name VARCHAR(100),
    form_of_address VARCHAR(20),
    industry VARCHAR(10),
    last_change_date TIMESTAMP,
    last_name VARCHAR(100),
    organization_bp_name1 VARCHAR(200),
    organization_bp_name2 VARCHAR(200),
    business_partner_is_blocked VARCHAR(5),
    is_marked_for_archiving VARCHAR(5)
);

CREATE TABLE IF NOT EXISTS business_partner_addresses (
    business_partner VARCHAR(20),
    address_id VARCHAR(20),
    validity_start_date TIMESTAMP,
    validity_end_date TIMESTAMP,
    address_time_zone VARCHAR(10),
    city_name VARCHAR(100),
    country VARCHAR(5),
    postal_code VARCHAR(20),
    region VARCHAR(10),
    street_name VARCHAR(200),
    transport_zone VARCHAR(20),
    PRIMARY KEY (business_partner, address_id)
);

CREATE TABLE IF NOT EXISTS customer_company_assignments (
    customer VARCHAR(20),
    company_code VARCHAR(10),
    payment_terms VARCHAR(10),
    reconciliation_account VARCHAR(20),
    deletion_indicator VARCHAR(5),
    customer_account_group VARCHAR(10),
    PRIMARY KEY (customer, company_code)
);

CREATE TABLE IF NOT EXISTS customer_sales_area_assignments (
    customer VARCHAR(20),
    sales_organization VARCHAR(10),
    distribution_channel VARCHAR(5),
    division VARCHAR(5),
    billing_is_blocked_for_customer VARCHAR(5),
    currency VARCHAR(5),
    customer_payment_terms VARCHAR(10),
    delivery_priority VARCHAR(5),
    shipping_condition VARCHAR(5),
    PRIMARY KEY (customer, sales_organization, distribution_channel, division)
);

CREATE TABLE IF NOT EXISTS products (
    product VARCHAR(30) PRIMARY KEY,
    product_type VARCHAR(10),
    cross_plant_status VARCHAR(5),
    creation_date TIMESTAMP,
    created_by_user VARCHAR(20),
    last_change_date TIMESTAMP,
    is_marked_for_deletion VARCHAR(5),
    gross_weight NUMERIC(18,3),
    weight_unit VARCHAR(5),
    net_weight NUMERIC(18,3),
    product_group VARCHAR(10),
    base_unit VARCHAR(5),
    division VARCHAR(5),
    industry_sector VARCHAR(5)
);

CREATE TABLE IF NOT EXISTS product_descriptions (
    product VARCHAR(30),
    language VARCHAR(5),
    product_description VARCHAR(500),
    PRIMARY KEY (product, language)
);

CREATE TABLE IF NOT EXISTS plants (
    plant VARCHAR(10) PRIMARY KEY,
    plant_name VARCHAR(100),
    valuation_area VARCHAR(10),
    plant_customer VARCHAR(20),
    plant_supplier VARCHAR(20),
    factory_calendar VARCHAR(10),
    sales_organization VARCHAR(10),
    address_id VARCHAR(20),
    plant_category VARCHAR(5),
    distribution_channel VARCHAR(5),
    division VARCHAR(5),
    language VARCHAR(5)
);

CREATE TABLE IF NOT EXISTS product_plants (
    product VARCHAR(30),
    plant VARCHAR(10),
    country_of_origin VARCHAR(5),
    profit_center VARCHAR(20),
    mrp_type VARCHAR(5),
    PRIMARY KEY (product, plant)
);

-- ========================
-- GRAPH TABLES
-- ========================

CREATE TABLE IF NOT EXISTS graph_nodes (
    id VARCHAR(50) PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    label VARCHAR(200),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS graph_edges (
    id SERIAL PRIMARY KEY,
    source_id VARCHAR(50) NOT NULL,
    target_id VARCHAR(50) NOT NULL,
    relationship VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for fast graph traversal
CREATE INDEX IF NOT EXISTS idx_graph_edges_source ON graph_edges(source_id);
CREATE INDEX IF NOT EXISTS idx_graph_edges_target ON graph_edges(target_id);
CREATE INDEX IF NOT EXISTS idx_graph_nodes_type ON graph_nodes(type);
CREATE INDEX IF NOT EXISTS idx_sales_order_sold_to ON sales_order_headers(sold_to_party);
CREATE INDEX IF NOT EXISTS idx_billing_sold_to ON billing_document_headers(sold_to_party);
CREATE INDEX IF NOT EXISTS idx_delivery_items_ref ON outbound_delivery_items(reference_sd_document);
CREATE INDEX IF NOT EXISTS idx_billing_items_ref ON billing_document_items(reference_sd_document);
CREATE INDEX IF NOT EXISTS idx_journal_ref ON journal_entry_items_ar(reference_document);
CREATE INDEX IF NOT EXISTS idx_payments_invoice ON payments_ar(invoice_reference);
