# Setup Guide

## Prerequisites
- Java 17+
- Maven 3.8+
- A Supabase account (free): https://supabase.com
- A Gemini API key (free): https://ai.google.dev

---

## Step 1 — Supabase PostgreSQL Setup

1. Go to https://supabase.com → New Project
2. Note down:
   - **Host**: `db.xxxxxxxxxxxx.supabase.co`
   - **Password**: your project password
3. Go to **SQL Editor** in Supabase dashboard
4. Paste and run the entire contents of `scripts/schema.sql`
5. All tables will be created

---

## Step 2 — Configure the Application

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://db.YOUR_PROJECT_REF.supabase.co:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=YOUR_SUPABASE_PASSWORD

gemini.api.key=YOUR_GEMINI_API_KEY
```

---

## Step 3 — Place the Dataset

```
sap-o2c-graph/
└── data/
    └── sap-o2c-data/
        ├── sales_order_headers/
        ├── billing_document_headers/
        ├── ... (all folders from the zip)
```

Extract the downloaded zip so the `sap-o2c-data` folder is at `./data/sap-o2c-data` relative to project root.

Or update `data.path` in `application.properties` to the absolute path of your extracted folder.

---

## Step 4 — Build and Run

```bash
cd sap-o2c-graph
mvn clean package -DskipTests
java -jar target/sap-o2c-graph-1.0.0.jar
```

Or with Maven directly:
```bash
mvn spring-boot:run
```

On **first startup**, the app will automatically:
1. Ingest all JSONL files into PostgreSQL (~1-2 minutes)
2. Build graph nodes and edges (~30 seconds)

Subsequent startups skip ingestion if data is already present.

---

## Step 5 — Open the App

Visit: http://localhost:8080

---

## Step 6 — Deploy to Railway (for demo link)

1. Push your code to GitHub (public repo)
2. Go to https://railway.app → New Project → Deploy from GitHub
3. Add environment variables in Railway dashboard:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_PASSWORD`
   - `GEMINI_API_KEY`
   - `DATA_PATH` → Set this to `/app/data/sap-o2c-data` and include data folder in repo, OR use a startup script

**Important for Railway**: Add the data folder to your repo or use Railway volumes.

---

## Troubleshooting

| Issue | Fix |
|---|---|
| `Connection refused` to Supabase | Check your DB URL format — use port 5432, not 6543 |
| Data not loading | Check `data.path` in properties — must point to folder containing `sales_order_headers/` etc. |
| Gemini returning errors | Verify API key is valid and has quota remaining |
| Graph shows no nodes | Check `/api/admin/status` — if `graphBuilt: false`, POST to `/api/admin/build-graph` |
| Slow first load | Normal — graph layout runs on ~60 nodes. Subsequent loads are cached |
