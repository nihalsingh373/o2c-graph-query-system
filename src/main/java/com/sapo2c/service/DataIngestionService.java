package com.sapo2c.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapo2c.model.GraphEdge;
import com.sapo2c.model.GraphNode;
import com.sapo2c.repository.GraphEdgeRepository;
import com.sapo2c.repository.GraphNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataIngestionService {

    private final JdbcTemplate jdbcTemplate;
    private final GraphNodeRepository nodeRepository;
    private final GraphEdgeRepository edgeRepository;
    private final ObjectMapper objectMapper;

    @Value("${data.path:./data/sap-o2c-data}")
    private String dataPath;

    public boolean isDataLoaded() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales_order_headers", Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isGraphBuilt() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM graph_nodes", Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public void ingestAllData() {
        log.info("Starting data ingestion from: {}", dataPath);
        ingestSalesOrderHeaders();
        ingestSalesOrderItems();
        ingestSalesOrderScheduleLines();
        ingestOutboundDeliveryHeaders();
        ingestOutboundDeliveryItems();
        ingestBillingDocumentHeaders();
        ingestBillingDocumentItems();
        ingestBillingDocumentCancellations();
        ingestJournalEntries();
        ingestPayments();
        ingestBusinessPartners();
        ingestBusinessPartnerAddresses();
        ingestCustomerCompanyAssignments();
        ingestCustomerSalesAreaAssignments();
        ingestProducts();
        ingestProductDescriptions();
        ingestPlants();
        log.info("Data ingestion complete.");
    }

    @Transactional
    public void buildGraph() {
        log.info("Building graph nodes and edges...");
        jdbcTemplate.execute("DELETE FROM graph_edges");
        jdbcTemplate.execute("DELETE FROM graph_nodes");

        buildSalesOrderNodes();
        buildCustomerNodes();
        buildDeliveryNodes();
        buildBillingNodes();
        buildJournalNodes();
        buildPaymentNodes();
        buildProductNodes();

        buildSalesOrderToCustomerEdges();
        buildSalesOrderToDeliveryEdges();
        buildSalesOrderToBillingEdges();
        buildBillingToJournalEdges();
        buildJournalToPaymentEdges();
        buildSalesOrderToProductEdges();

        log.info("Graph built: {} nodes, {} edges",
            nodeRepository.count(), edgeRepository.count());
    }

    // ========== INGESTION METHODS ==========

    private void ingestSalesOrderHeaders() {
        String sql = """
            INSERT INTO sales_order_headers (
                sales_order, sales_order_type, sales_organization, distribution_channel,
                organization_division, sales_group, sales_office, sold_to_party,
                creation_date, created_by_user, last_change_date_time, total_net_amount,
                overall_delivery_status, overall_ord_reltd_billg_status,
                overall_sd_doc_reference_status, transaction_currency,
                pricing_date, requested_delivery_date, header_billing_block_reason,
                delivery_block_reason, incoterms_classification, incoterms_location1,
                customer_payment_terms, total_credit_check_status
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (sales_order) DO NOTHING
            """;
        processJsonlFiles("sales_order_headers", node -> {
            jdbcTemplate.update(sql,
                getString(node, "salesOrder"), getString(node, "salesOrderType"),
                getString(node, "salesOrganization"), getString(node, "distributionChannel"),
                getString(node, "organizationDivision"), getString(node, "salesGroup"),
                getString(node, "salesOffice"), getString(node, "soldToParty"),
                parseTimestamp(node, "creationDate"), getString(node, "createdByUser"),
                parseTimestamp(node, "lastChangeDateTime"), getDecimal(node, "totalNetAmount"),
                getString(node, "overallDeliveryStatus"), getString(node, "overallOrdReltdBillgStatus"),
                getString(node, "overallSdDocReferenceStatus"), getString(node, "transactionCurrency"),
                parseTimestamp(node, "pricingDate"), parseTimestamp(node, "requestedDeliveryDate"),
                getString(node, "headerBillingBlockReason"), getString(node, "deliveryBlockReason"),
                getString(node, "incotermsClassification"), getString(node, "incotermsLocation1"),
                getString(node, "customerPaymentTerms"), getString(node, "totalCreditCheckStatus")
            );
        });
        log.info("Ingested sales_order_headers");
    }

    private void ingestSalesOrderItems() {
        String sql = """
            INSERT INTO sales_order_items (
                sales_order, sales_order_item, sales_order_item_category, material,
                requested_quantity, requested_quantity_unit, transaction_currency,
                net_amount, material_group, production_plant, storage_location,
                sales_document_rjcn_reason, item_billing_block_reason
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (sales_order, sales_order_item) DO NOTHING
            """;
        processJsonlFiles("sales_order_items", node -> {
            jdbcTemplate.update(sql,
                getString(node, "salesOrder"), getString(node, "salesOrderItem"),
                getString(node, "salesOrderItemCategory"), getString(node, "material"),
                getDecimal(node, "requestedQuantity"), getString(node, "requestedQuantityUnit"),
                getString(node, "transactionCurrency"), getDecimal(node, "netAmount"),
                getString(node, "materialGroup"), getString(node, "productionPlant"),
                getString(node, "storageLocation"), getString(node, "salesDocumentRjcnReason"),
                getString(node, "itemBillingBlockReason")
            );
        });
        log.info("Ingested sales_order_items");
    }

    private void ingestSalesOrderScheduleLines() {
        String sql = """
            INSERT INTO sales_order_schedule_lines (
                sales_order, sales_order_item, schedule_line, confirmed_delivery_date,
                order_quantity_unit, confd_order_qty_by_matl_avail_check
            ) VALUES (?,?,?,?,?,?)
            ON CONFLICT (sales_order, sales_order_item, schedule_line) DO NOTHING
            """;
        processJsonlFiles("sales_order_schedule_lines", node -> {
            jdbcTemplate.update(sql,
                getString(node, "salesOrder"), getString(node, "salesOrderItem"),
                getString(node, "scheduleLine"), parseTimestamp(node, "confirmedDeliveryDate"),
                getString(node, "orderQuantityUnit"),
                getDecimal(node, "confdOrderQtyByMatlAvailCheck")
            );
        });
        log.info("Ingested sales_order_schedule_lines");
    }

    private void ingestOutboundDeliveryHeaders() {
        String sql = """
            INSERT INTO outbound_delivery_headers (
                delivery_document, actual_goods_movement_date, creation_date,
                delivery_block_reason, hdr_general_incompletion_status,
                header_billing_block_reason, last_change_date,
                overall_goods_movement_status, overall_picking_status,
                overall_proof_of_delivery_status, shipping_point
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (delivery_document) DO NOTHING
            """;
        processJsonlFiles("outbound_delivery_headers", node -> {
            jdbcTemplate.update(sql,
                getString(node, "deliveryDocument"),
                parseTimestamp(node, "actualGoodsMovementDate"),
                parseTimestamp(node, "creationDate"),
                getString(node, "deliveryBlockReason"),
                getString(node, "hdrGeneralIncompletionStatus"),
                getString(node, "headerBillingBlockReason"),
                parseTimestamp(node, "lastChangeDate"),
                getString(node, "overallGoodsMovementStatus"),
                getString(node, "overallPickingStatus"),
                getString(node, "overallProofOfDeliveryStatus"),
                getString(node, "shippingPoint")
            );
        });
        log.info("Ingested outbound_delivery_headers");
    }

    private void ingestOutboundDeliveryItems() {
        String sql = """
            INSERT INTO outbound_delivery_items (
                delivery_document, delivery_document_item, actual_delivery_quantity,
                batch, delivery_quantity_unit, item_billing_block_reason,
                last_change_date, plant, reference_sd_document,
                reference_sd_document_item, storage_location
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (delivery_document, delivery_document_item) DO NOTHING
            """;
        processJsonlFiles("outbound_delivery_items", node -> {
            jdbcTemplate.update(sql,
                getString(node, "deliveryDocument"), getString(node, "deliveryDocumentItem"),
                getDecimal(node, "actualDeliveryQuantity"), getString(node, "batch"),
                getString(node, "deliveryQuantityUnit"), getString(node, "itemBillingBlockReason"),
                parseTimestamp(node, "lastChangeDate"), getString(node, "plant"),
                getString(node, "referenceSdDocument"), getString(node, "referenceSdDocumentItem"),
                getString(node, "storageLocation")
            );
        });
        log.info("Ingested outbound_delivery_items");
    }

    private void ingestBillingDocumentHeaders() {
        String sql = """
            INSERT INTO billing_document_headers (
                billing_document, billing_document_type, creation_date,
                billing_document_date, billing_document_is_cancelled,
                cancelled_billing_document, total_net_amount, transaction_currency,
                company_code, fiscal_year, accounting_document, sold_to_party
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (billing_document) DO NOTHING
            """;
        processJsonlFiles("billing_document_headers", node -> {
            jdbcTemplate.update(sql,
                getString(node, "billingDocument"), getString(node, "billingDocumentType"),
                parseTimestamp(node, "creationDate"),
                parseTimestamp(node, "billingDocumentDate"),
                getString(node, "billingDocumentIsCancelled"),
                getString(node, "cancelledBillingDocument"),
                getDecimal(node, "totalNetAmount"), getString(node, "transactionCurrency"),
                getString(node, "companyCode"), getString(node, "fiscalYear"),
                getString(node, "accountingDocument"), getString(node, "soldToParty")
            );
        });
        log.info("Ingested billing_document_headers");
    }

    private void ingestBillingDocumentItems() {
        String sql = """
            INSERT INTO billing_document_items (
                billing_document, billing_document_item, material, billing_quantity,
                billing_quantity_unit, net_amount, transaction_currency,
                reference_sd_document, reference_sd_document_item
            ) VALUES (?,?,?,?,?,?,?,?,?)
            ON CONFLICT (billing_document, billing_document_item) DO NOTHING
            """;
        processJsonlFiles("billing_document_items", node -> {
            jdbcTemplate.update(sql,
                getString(node, "billingDocument"), getString(node, "billingDocumentItem"),
                getString(node, "material"), getDecimal(node, "billingQuantity"),
                getString(node, "billingQuantityUnit"), getDecimal(node, "netAmount"),
                getString(node, "transactionCurrency"), getString(node, "referenceSdDocument"),
                getString(node, "referenceSdDocumentItem")
            );
        });
        log.info("Ingested billing_document_items");
    }

    private void ingestBillingDocumentCancellations() {
        String sql = """
            INSERT INTO billing_document_cancellations (
                billing_document, billing_document_type, creation_date,
                billing_document_date, billing_document_is_cancelled,
                cancelled_billing_document, total_net_amount, transaction_currency,
                company_code, fiscal_year, accounting_document, sold_to_party
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (billing_document) DO NOTHING
            """;
        processJsonlFiles("billing_document_cancellations", node -> {
            jdbcTemplate.update(sql,
                getString(node, "billingDocument"), getString(node, "billingDocumentType"),
                parseTimestamp(node, "creationDate"),
                parseTimestamp(node, "billingDocumentDate"),
                getString(node, "billingDocumentIsCancelled"),
                getString(node, "cancelledBillingDocument"),
                getDecimal(node, "totalNetAmount"), getString(node, "transactionCurrency"),
                getString(node, "companyCode"), getString(node, "fiscalYear"),
                getString(node, "accountingDocument"), getString(node, "soldToParty")
            );
        });
        log.info("Ingested billing_document_cancellations");
    }

    private void ingestJournalEntries() {
        String sql = """
            INSERT INTO journal_entry_items_ar (
                company_code, fiscal_year, accounting_document, accounting_document_item,
                gl_account, reference_document, cost_center, profit_center,
                transaction_currency, amount_in_transaction_currency,
                company_code_currency, amount_in_company_code_currency,
                posting_date, document_date, accounting_document_type,
                assignment_reference, customer, financial_account_type,
                clearing_date, clearing_accounting_document, clearing_doc_fiscal_year
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (company_code, fiscal_year, accounting_document, accounting_document_item) DO NOTHING
            """;
        processJsonlFiles("journal_entry_items_accounts_receivable", node -> {
            jdbcTemplate.update(sql,
                getString(node, "companyCode"), getString(node, "fiscalYear"),
                getString(node, "accountingDocument"), getString(node, "accountingDocumentItem"),
                getString(node, "glAccount"), getString(node, "referenceDocument"),
                getString(node, "costCenter"), getString(node, "profitCenter"),
                getString(node, "transactionCurrency"),
                getDecimal(node, "amountInTransactionCurrency"),
                getString(node, "companyCodeCurrency"),
                getDecimal(node, "amountInCompanyCodeCurrency"),
                parseTimestamp(node, "postingDate"), parseTimestamp(node, "documentDate"),
                getString(node, "accountingDocumentType"),
                getString(node, "assignmentReference"), getString(node, "customer"),
                getString(node, "financialAccountType"),
                parseTimestamp(node, "clearingDate"),
                getString(node, "clearingAccountingDocument"),
                getString(node, "clearingDocFiscalYear")
            );
        });
        log.info("Ingested journal_entry_items_ar");
    }

    private void ingestPayments() {
        String sql = """
            INSERT INTO payments_ar (
                company_code, fiscal_year, accounting_document, accounting_document_item,
                clearing_date, clearing_accounting_document, clearing_doc_fiscal_year,
                amount_in_transaction_currency, transaction_currency,
                amount_in_company_code_currency, company_code_currency,
                customer, invoice_reference, invoice_reference_fiscal_year,
                sales_document, sales_document_item, posting_date, document_date,
                assignment_reference, gl_account, financial_account_type,
                profit_center, cost_center
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (company_code, fiscal_year, accounting_document, accounting_document_item) DO NOTHING
            """;
        processJsonlFiles("payments_accounts_receivable", node -> {
            jdbcTemplate.update(sql,
                getString(node, "companyCode"), getString(node, "fiscalYear"),
                getString(node, "accountingDocument"), getString(node, "accountingDocumentItem"),
                parseTimestamp(node, "clearingDate"),
                getString(node, "clearingAccountingDocument"),
                getString(node, "clearingDocFiscalYear"),
                getDecimal(node, "amountInTransactionCurrency"),
                getString(node, "transactionCurrency"),
                getDecimal(node, "amountInCompanyCodeCurrency"),
                getString(node, "companyCodeCurrency"), getString(node, "customer"),
                getString(node, "invoiceReference"),
                getString(node, "invoiceReferenceFiscalYear"),
                getString(node, "salesDocument"), getString(node, "salesDocumentItem"),
                parseTimestamp(node, "postingDate"), parseTimestamp(node, "documentDate"),
                getString(node, "assignmentReference"), getString(node, "glAccount"),
                getString(node, "financialAccountType"), getString(node, "profitCenter"),
                getString(node, "costCenter")
            );
        });
        log.info("Ingested payments_ar");
    }

    private void ingestBusinessPartners() {
        String sql = """
            INSERT INTO business_partners (
                business_partner, customer, business_partner_category,
                business_partner_full_name, business_partner_grouping,
                business_partner_name, correspondence_language, created_by_user,
                creation_date, first_name, form_of_address, industry,
                last_change_date, last_name, organization_bp_name1,
                organization_bp_name2, business_partner_is_blocked, is_marked_for_archiving
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (business_partner) DO NOTHING
            """;
        processJsonlFiles("business_partners", node -> {
            jdbcTemplate.update(sql,
                getString(node, "businessPartner"), getString(node, "customer"),
                getString(node, "businessPartnerCategory"),
                getString(node, "businessPartnerFullName"),
                getString(node, "businessPartnerGrouping"),
                getString(node, "businessPartnerName"),
                getString(node, "correspondenceLanguage"), getString(node, "createdByUser"),
                parseTimestamp(node, "creationDate"), getString(node, "firstName"),
                getString(node, "formOfAddress"), getString(node, "industry"),
                parseTimestamp(node, "lastChangeDate"), getString(node, "lastName"),
                getString(node, "organizationBpName1"), getString(node, "organizationBpName2"),
                getString(node, "businessPartnerIsBlocked"),
                getString(node, "isMarkedForArchiving")
            );
        });
        log.info("Ingested business_partners");
    }

    private void ingestBusinessPartnerAddresses() {
        String sql = """
            INSERT INTO business_partner_addresses (
                business_partner, address_id, validity_start_date, validity_end_date,
                address_time_zone, city_name, country, postal_code, region,
                street_name, transport_zone
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (business_partner, address_id) DO NOTHING
            """;
        processJsonlFiles("business_partner_addresses", node -> {
            jdbcTemplate.update(sql,
                getString(node, "businessPartner"), getString(node, "addressId"),
                parseTimestamp(node, "validityStartDate"),
                parseTimestamp(node, "validityEndDate"),
                getString(node, "addressTimeZone"), getString(node, "cityName"),
                getString(node, "country"), getString(node, "postalCode"),
                getString(node, "region"), getString(node, "streetName"),
                getString(node, "transportZone")
            );
        });
        log.info("Ingested business_partner_addresses");
    }

    private void ingestCustomerCompanyAssignments() {
        String sql = """
            INSERT INTO customer_company_assignments (
                customer, company_code, payment_terms, reconciliation_account,
                deletion_indicator, customer_account_group
            ) VALUES (?,?,?,?,?,?)
            ON CONFLICT (customer, company_code) DO NOTHING
            """;
        processJsonlFiles("customer_company_assignments", node -> {
            jdbcTemplate.update(sql,
                getString(node, "customer"), getString(node, "companyCode"),
                getString(node, "paymentTerms"), getString(node, "reconciliationAccount"),
                getString(node, "deletionIndicator"), getString(node, "customerAccountGroup")
            );
        });
        log.info("Ingested customer_company_assignments");
    }

    private void ingestCustomerSalesAreaAssignments() {
        String sql = """
            INSERT INTO customer_sales_area_assignments (
                customer, sales_organization, distribution_channel, division,
                billing_is_blocked_for_customer, currency, customer_payment_terms,
                delivery_priority, shipping_condition
            ) VALUES (?,?,?,?,?,?,?,?,?)
            ON CONFLICT (customer, sales_organization, distribution_channel, division) DO NOTHING
            """;
        processJsonlFiles("customer_sales_area_assignments", node -> {
            jdbcTemplate.update(sql,
                getString(node, "customer"), getString(node, "salesOrganization"),
                getString(node, "distributionChannel"), getString(node, "division"),
                getString(node, "billingIsBlockedForCustomer"), getString(node, "currency"),
                getString(node, "customerPaymentTerms"), getString(node, "deliveryPriority"),
                getString(node, "shippingCondition")
            );
        });
        log.info("Ingested customer_sales_area_assignments");
    }

    private void ingestProducts() {
        String sql = """
            INSERT INTO products (
                product, product_type, cross_plant_status, creation_date,
                created_by_user, last_change_date, is_marked_for_deletion,
                gross_weight, weight_unit, net_weight, product_group,
                base_unit, division, industry_sector
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (product) DO NOTHING
            """;
        processJsonlFiles("products", node -> {
            jdbcTemplate.update(sql,
                getString(node, "product"), getString(node, "productType"),
                getString(node, "crossPlantStatus"), parseTimestamp(node, "creationDate"),
                getString(node, "createdByUser"), parseTimestamp(node, "lastChangeDate"),
                getString(node, "isMarkedForDeletion"), getDecimal(node, "grossWeight"),
                getString(node, "weightUnit"), getDecimal(node, "netWeight"),
                getString(node, "productGroup"), getString(node, "baseUnit"),
                getString(node, "division"), getString(node, "industrySector")
            );
        });
        log.info("Ingested products");
    }

    private void ingestProductDescriptions() {
        String sql = """
            INSERT INTO product_descriptions (product, language, product_description)
            VALUES (?,?,?)
            ON CONFLICT (product, language) DO NOTHING
            """;
        processJsonlFiles("product_descriptions", node -> {
            jdbcTemplate.update(sql,
                getString(node, "product"), getString(node, "language"),
                getString(node, "productDescription")
            );
        });
        log.info("Ingested product_descriptions");
    }

    private void ingestPlants() {
        String sql = """
            INSERT INTO plants (
                plant, plant_name, valuation_area, plant_customer, plant_supplier,
                factory_calendar, sales_organization, address_id, plant_category,
                distribution_channel, division, language
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (plant) DO NOTHING
            """;
        processJsonlFiles("plants", node -> {
            jdbcTemplate.update(sql,
                getString(node, "plant"), getString(node, "plantName"),
                getString(node, "valuationArea"), getString(node, "plantCustomer"),
                getString(node, "plantSupplier"), getString(node, "factoryCalendar"),
                getString(node, "salesOrganization"), getString(node, "addressId"),
                getString(node, "plantCategory"), getString(node, "distributionChannel"),
                getString(node, "division"), getString(node, "language")
            );
        });
        log.info("Ingested plants");
    }

    // ========== GRAPH BUILDING METHODS ==========

    private void buildSalesOrderNodes() {
        String sql = """
            SELECT sales_order, sold_to_party, total_net_amount, transaction_currency,
                   overall_delivery_status, overall_ord_reltd_billg_status, creation_date
            FROM sales_order_headers
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<GraphNode> nodes = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String id = "SO-" + row.get("sales_order");
            Map<String, Object> meta = new HashMap<>(row);
            nodes.add(GraphNode.builder()
                .id(id)
                .type("salesOrder")
                .label("Sales Order " + row.get("sales_order"))
                .metadata(meta)
                .build());
        }
        nodeRepository.saveAll(nodes);
        log.info("Built {} salesOrder nodes", nodes.size());
    }

    private void buildCustomerNodes() {
        String sql = """
            SELECT bp.business_partner, bp.customer, bp.business_partner_full_name,
                   bp.business_partner_name, bp.organization_bp_name1,
                   bpa.city_name, bpa.country
            FROM business_partners bp
            LEFT JOIN business_partner_addresses bpa ON bp.business_partner = bpa.business_partner
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<GraphNode> nodes = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String bp = String.valueOf(row.get("business_partner"));
            if (seen.contains(bp)) continue;
            seen.add(bp);
            String name = row.get("business_partner_full_name") != null
                ? String.valueOf(row.get("business_partner_full_name"))
                : row.get("organization_bp_name1") != null
                    ? String.valueOf(row.get("organization_bp_name1"))
                    : "Customer " + row.get("customer");
            nodes.add(GraphNode.builder()
                .id("CUST-" + bp)
                .type("customer")
                .label(name)
                .metadata(new HashMap<>(row))
                .build());
        }
        nodeRepository.saveAll(nodes);
        log.info("Built {} customer nodes", nodes.size());
    }

    private void buildDeliveryNodes() {
        String sql = "SELECT * FROM outbound_delivery_headers";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<GraphNode> nodes = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            nodes.add(GraphNode.builder()
                .id("DEL-" + row.get("delivery_document"))
                .type("delivery")
                .label("Delivery " + row.get("delivery_document"))
                .metadata(new HashMap<>(row))
                .build());
        }
        nodeRepository.saveAll(nodes);
        log.info("Built {} delivery nodes", nodes.size());
    }

    private void buildBillingNodes() {
        String sql = "SELECT * FROM billing_document_headers";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<GraphNode> nodes = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            nodes.add(GraphNode.builder()
                .id("BILL-" + row.get("billing_document"))
                .type("billing")
                .label("Invoice " + row.get("billing_document"))
                .metadata(new HashMap<>(row))
                .build());
        }
        nodeRepository.saveAll(nodes);
        log.info("Built {} billing nodes", nodes.size());
    }

    private void buildJournalNodes() {
        String sql = """
            SELECT DISTINCT accounting_document, company_code, fiscal_year,
                   reference_document, customer, posting_date,
                   SUM(amount_in_transaction_currency) as total_amount,
                   transaction_currency
            FROM journal_entry_items_ar
            GROUP BY accounting_document, company_code, fiscal_year,
                     reference_document, customer, posting_date, transaction_currency
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<GraphNode> nodes = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String id = "JE-" + row.get("accounting_document");
            if (seen.contains(id)) continue;
            seen.add(id);
            nodes.add(GraphNode.builder()
                .id(id)
                .type("journalEntry")
                .label("Journal Entry " + row.get("accounting_document"))
                .metadata(new HashMap<>(row))
                .build());
        }
        nodeRepository.saveAll(nodes);
        log.info("Built {} journalEntry nodes", nodes.size());
    }

    private void buildPaymentNodes() {
        String sql = """
            SELECT DISTINCT accounting_document, company_code, fiscal_year,
                   customer, clearing_date, invoice_reference, sales_document,
                   SUM(amount_in_transaction_currency) as total_amount,
                   transaction_currency
            FROM payments_ar
            GROUP BY accounting_document, company_code, fiscal_year,
                     customer, clearing_date, invoice_reference, sales_document, transaction_currency
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<GraphNode> nodes = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String id = "PAY-" + row.get("accounting_document");
            if (seen.contains(id)) continue;
            seen.add(id);
            nodes.add(GraphNode.builder()
                .id(id)
                .type("payment")
                .label("Payment " + row.get("accounting_document"))
                .metadata(new HashMap<>(row))
                .build());
        }
        nodeRepository.saveAll(nodes);
        log.info("Built {} payment nodes", nodes.size());
    }

    private void buildProductNodes() {
        String sql = """
            SELECT p.product, p.product_type, p.product_group, pd.product_description
            FROM products p
            LEFT JOIN product_descriptions pd ON p.product = pd.product AND pd.language = 'EN'
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<GraphNode> nodes = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String desc = row.get("product_description") != null
                ? String.valueOf(row.get("product_description"))
                : String.valueOf(row.get("product"));
            nodes.add(GraphNode.builder()
                .id("PROD-" + row.get("product"))
                .type("product")
                .label(desc)
                .metadata(new HashMap<>(row))
                .build());
        }
        nodeRepository.saveAll(nodes);
        log.info("Built {} product nodes", nodes.size());
    }

    private void buildSalesOrderToCustomerEdges() {
        String sql = """
            SELECT soh.sales_order, bp.business_partner
            FROM sales_order_headers soh
            JOIN business_partners bp ON bp.customer = soh.sold_to_party
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<GraphEdge> edges = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            edges.add(GraphEdge.builder()
                .sourceId("SO-" + row.get("sales_order"))
                .targetId("CUST-" + row.get("business_partner"))
                .relationship("SOLD_TO")
                .build());
        }
        edgeRepository.saveAll(edges);
        log.info("Built {} SOLD_TO edges", edges.size());
    }

    private void buildSalesOrderToDeliveryEdges() {
        String sql = """
            SELECT DISTINCT odi.reference_sd_document as sales_order, odh.delivery_document
            FROM outbound_delivery_items odi
            JOIN outbound_delivery_headers odh ON odi.delivery_document = odh.delivery_document
            WHERE odi.reference_sd_document IS NOT NULL AND odi.reference_sd_document != ''
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<GraphEdge> edges = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            edges.add(GraphEdge.builder()
                .sourceId("SO-" + row.get("sales_order"))
                .targetId("DEL-" + row.get("delivery_document"))
                .relationship("DELIVERED_BY")
                .build());
        }
        edgeRepository.saveAll(edges);
        log.info("Built {} DELIVERED_BY edges", edges.size());
    }

    private void buildSalesOrderToBillingEdges() {
        String sql = """
            SELECT DISTINCT bdi.reference_sd_document as sales_order, bdh.billing_document
            FROM billing_document_items bdi
            JOIN billing_document_headers bdh ON bdi.billing_document = bdh.billing_document
            WHERE bdi.reference_sd_document IS NOT NULL AND bdi.reference_sd_document != ''
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<GraphEdge> edges = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            edges.add(GraphEdge.builder()
                .sourceId("SO-" + row.get("sales_order"))
                .targetId("BILL-" + row.get("billing_document"))
                .relationship("BILLED_AS")
                .build());
        }
        edgeRepository.saveAll(edges);
        log.info("Built {} BILLED_AS edges", edges.size());
    }

    private void buildBillingToJournalEdges() {
        String sql = """
            SELECT DISTINCT bdh.billing_document, je.accounting_document
            FROM billing_document_headers bdh
            JOIN journal_entry_items_ar je ON je.reference_document = bdh.billing_document
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<GraphEdge> edges = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            edges.add(GraphEdge.builder()
                .sourceId("BILL-" + row.get("billing_document"))
                .targetId("JE-" + row.get("accounting_document"))
                .relationship("HAS_JOURNAL_ENTRY")
                .build());
        }
        edgeRepository.saveAll(edges);
        log.info("Built {} HAS_JOURNAL_ENTRY edges", edges.size());
    }

    private void buildJournalToPaymentEdges() {
        String sql = """
            SELECT DISTINCT je.accounting_document as journal_doc, p.accounting_document as payment_doc
            FROM journal_entry_items_ar je
            JOIN payments_ar p ON p.invoice_reference = je.accounting_document
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<GraphEdge> edges = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            edges.add(GraphEdge.builder()
                .sourceId("JE-" + row.get("journal_doc"))
                .targetId("PAY-" + row.get("payment_doc"))
                .relationship("PAID_BY")
                .build());
        }
        edgeRepository.saveAll(edges);
        log.info("Built {} PAID_BY edges", edges.size());
    }

    private void buildSalesOrderToProductEdges() {
        String sql = """
            SELECT DISTINCT sales_order, material FROM sales_order_items
            WHERE material IS NOT NULL AND material != ''
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<GraphEdge> edges = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            edges.add(GraphEdge.builder()
                .sourceId("SO-" + row.get("sales_order"))
                .targetId("PROD-" + row.get("material"))
                .relationship("CONTAINS_PRODUCT")
                .build());
        }
        edgeRepository.saveAll(edges);
        log.info("Built {} CONTAINS_PRODUCT edges", edges.size());
    }

    // ========== UTILITY METHODS ==========

    @FunctionalInterface
    interface JsonNodeProcessor {
        void process(JsonNode node) throws Exception;
    }

    private void processJsonlFiles(String folderName, JsonNodeProcessor processor) {
        File folder = new File(dataPath + "/" + folderName);
        if (!folder.exists() || !folder.isDirectory()) {
            log.warn("Folder not found: {}", folder.getAbsolutePath());
            return;
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".jsonl"));
        if (files == null) return;

        int processed = 0;
        for (File file : files) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    try {
                        JsonNode node = objectMapper.readTree(line);
                        processor.process(node);
                        processed++;
                    } catch (Exception e) {
                        log.warn("Error processing line in {}: {}", file.getName(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Error reading file {}: {}", file.getName(), e.getMessage());
            }
        }
        log.debug("Processed {} records from {}", processed, folderName);
    }

    private String getString(JsonNode node, String field) {
        JsonNode val = node.get(field);
        if (val == null || val.isNull()) return null;
        String s = val.asText().trim();
        return s.isEmpty() ? null : s;
    }

    private java.math.BigDecimal getDecimal(JsonNode node, String field) {
        JsonNode val = node.get(field);
        if (val == null || val.isNull()) return null;
        String s = val.asText().trim();
        if (s.isEmpty()) return null;
        try {
            return new java.math.BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private java.sql.Timestamp parseTimestamp(JsonNode node, String field) {
        String s = getString(node, field);
        if (s == null) return null;
        try {
            return java.sql.Timestamp.from(
                java.time.Instant.parse(s.endsWith("Z") ? s : s + "Z"));
        } catch (Exception e) {
            return null;
        }
    }
}
