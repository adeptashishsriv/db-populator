# DB Explorer - User Guide

DB Explorer is a modern, lightweight, and powerful database management tool designed for developers. It supports a wide range of relational databases and AWS DynamoDB, featuring an AI-powered SQL assistant to boost productivity.

---

## 🚀 Key Features

### 1. Unified Database Connectivity
Connect to multiple database types including PostgreSQL, MySQL, Oracle, SQL Server, SQLite, and AWS DynamoDB.
> ![Screenshot: Connection Manager](placeholder_connection_manager.png)
> *Manage all your database connections in one place with secure credential storage.*

### 2. AI SQL Assistant 🤖
Generate complex SQL queries using natural language. Simply describe what you want, and the AI will use your database schema to write the query for you.
> ![Screenshot: AI Assistant](placeholder_ai_assistant.png)
> *Convert "Show me top 10 customers by sales in 2023" directly into SQL.*

### 3. Advanced SQL Editor
Write and execute queries with full syntax highlighting, SQL auto-completion, and multi-tab support.
> ![Screenshot: SQL Editor](placeholder_sql_editor.png)
> *Professional editor with auto-completion for tables and columns.*

### 4. Health & Performance Dashboard
Monitor your database health in real-time with built-in metrics and performance indicators.
> ![Screenshot: Health Dashboard](placeholder_dashboard.png)
> *Keep an eye on connection health and execution stats.*

### 5. Schema Explorer & Visualization
Browse your database structure with a hierarchical tree view and generate interactive schema diagrams.
> ![Screenshot: Schema Diagram](placeholder_schema_diagram.png)
> *Visualize table relationships and explore column details easily.*

### 6. Data Export & DDL Generation
Export table data to CSV, SQL Insert statements, or SQL Update statements. Generate DDL for tables and materialized views with one click.
> ![Screenshot: Export Dialog](placeholder_export.png)
> *Flexible export options for data migration and backup.*

### 7. Explain Plan
Analyze query performance by viewing the execution plan directly within the tool.
> ![Screenshot: Explain Plan](placeholder_explain_plan.png)
> *Optimize your queries by understanding how the database executes them.*

### 8. Customizable Themes
Switch between various themes (Light, Dark, Material) with smooth transition animations.
> ![Screenshot: Theme Selection](placeholder_themes.png)
> *Choose the look that suits your environment.*

---

## 🛠 AI Configuration
To enable the AI Assistant, navigate to **Edit → AI Configuration**.
- **Provider:** OpenAI (Default)
- **Model:** Compatible with `gpt-4`, `gpt-3.5-turbo`, or custom endpoints.
- **Gemini Support:** While the current interface is optimized for OpenAI-compatible APIs, it can be used with Gemini through compatible gateway proxies or by updating the base URL in configuration.

---

## 🖥 System Monitoring
The status bar includes a real-time **JVM Heap Monitor** and a manual **Garbage Collection** trigger to ensure the application remains responsive even with large datasets.
