# SAP O2C Graph System

A graph-based data modeling and query system for SAP Order-to-Cash data, built with Java Spring Boot, PostgreSQL (Supabase), and Gemini AI.

---

## Architecture

```
Browser (Cytoscape.js + Chat UI)
        ↓ REST API
Spring Boot Backend (Java 17)
  ├── /api/graph     → Graph nodes + edges
  ├── /api/chat      → NL → SQL → Answer
  └── /api/admin     → Ingestion + graph build
        ↓
Supabase PostgreSQL
  ├── Raw tables (16 entities from JSONL)
  ├── graph_nodes (id, type, label, metadata JSONB)
  └── graph_edges (source, target, relationship)
        ↓
Gemini 1.5 Flash (free tier)
  └── NL → SQL generation + natural language answers
```

---

## Database Choice

**PostgreSQL via Supabase** was chosen because:
- The O2C data is highly relational — JOINs across orders → deliveries → billing → payments are natural SQL operations
- JSONB columns allow flexible metadata storage on graph nodes without rigid schema changes
- Supabase provides a free hosted PostgreSQL instance with a full REST/SQL interface
- Index support enables fast graph traversal queries

---

## Graph Modeling

| Node Type | Source Table | ID Prefix |
|---|---|---|
| salesOrder | sales_order_headers | SO- |
| customer | business_partners | CUST- |
| delivery | outbound_delivery_headers | DEL- |
| billing | billing_document_headers | BILL- |
| journalEntry | journal_entry_items_ar | JE- |
| payment | payments_ar | PAY- |
| product | products | PROD- |

**Edges (Relationships):**

| Relationship | From | To | Join Key |
|---|---|---|---|
| SOLD_TO | salesOrder | customer | sold_to_party → customer |
| DELIVERED_BY | salesOrder | delivery | outbound_delivery_items.reference_sd_document |
| BILLED_AS | salesOrder | billing | billing_document_items.reference_sd_document |
| HAS_JOURNAL_ENTRY | billing | journalEntry | accounting_document |
| PAID_BY | journalEntry | payment | invoice_reference |
| CONTAINS_PRODUCT | salesOrder | product | sales_order_items.material |

---

## LLM Prompting Strategy

The system uses a **two-pass LLM approach**:

**Pass 1 — Classification + SQL Generation:**
- System prompt includes full schema context (all 16 tables + column names + relationships)
- LLM is instructed to return structured JSON: `{ relevant, sql, answer_template }`
- Temperature = 0.1 for deterministic SQL output
- Guardrail is enforced at prompt level: irrelevant queries return `relevant: false`

**Pass 2 — Natural Language Answer:**
- SQL is executed against PostgreSQL
- Results (up to 20 rows) are passed back to Gemini
- LLM formulates a concise natural language answer grounded in actual data

---

## Guardrails

1. **Prompt-level guardrail**: System prompt explicitly instructs the LLM to classify queries as relevant/irrelevant before generating SQL
2. **SQL safety**: Backend only executes `SELECT` / `WITH` statements — any other SQL is blocked
3. **Result limit**: All queries are capped at 100 rows via automatic `LIMIT` injection
4. **No hallucination**: Answers are generated only after executing real queries — the LLM cannot fabricate data

---

## Setup Instructions

See SETUP.md for step-by-step instructions.

---

## Bonus Features Implemented

- ✅ Natural language to SQL translation (dynamic, not static)
- ✅ Node highlighting on graph after LLM response
- ✅ Conversation memory (last 6 turns sent to LLM)
- ✅ Graph node expansion (click any node to load neighbors)
- ✅ Node metadata inspection panel
- ✅ Full O2C flow tracing via SQL JOINs
