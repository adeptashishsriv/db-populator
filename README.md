# DB Explorer — Database Query Tool & Explorer

**Version:** 3.0.1 &nbsp; | &nbsp; **Released:** April 2026 &nbsp; | &nbsp; **By:** Ashish Srivastava &nbsp; | &nbsp; **©** 2026 Astro Adept AI Labs

---

## Overview

DB Explorer is a cross-platform database query tool and schema explorer for developers, DBAs, and data analysts. It provides a clean GUI for connecting to multiple databases, writing SQL, exploring schemas, monitoring health, and generating SQL using AI.

**Key capabilities:** Multi-database support (PostgreSQL, MySQL, tabs, each bound to its own connection. Syntax highlighting, keyword auto-complete, table/column name suggestions, undo/redo history, and Ctrl+Enter execution. Results load lazily in batches of 500 rows with sortable columns.

**Schema Explorer** — Browse connections, schemas, tables, views, materialized views, sequences, indexes, functions, and procedures in an expandable tree. Expand tables to see column definitions. Right-click for context actions: connect, open query tab, view data, export DDL, export data.

**Execution Plan Viewer** — Analyze query performance with database-native explain plans. PostgreSQL shows an interactive tree with cost/rows badges. MySQL renders a color-coded tabular view highlighting full scans. SQL Server and Oracle display formatted text plans.

**Schema Diagram** — Right-click any schema to open an interactive ER diagram. Drag tables to reposition, scroll to zoom, right-drag to pan, and click Fit to auto-layout. Foreign key relationships are drawn as connecting lines between tables.

**Data Export** — Export any table as DDL (CREATE TABLE), INSERT statements, UPDATE statements, or CSV. Exports stream to disk without holding data in memory. Copy to clipboard or save to file. Schema-level DDL export generates CREATE statements for all objects.

**AI SQL Generator** — Convert natural language descriptions into SQL using OpenAI, Claude, DeepSeek, Gemini, or any OpenAI-compatible endpoint. The generator is schema-aware and uses your actual table and column names. Configure via Settings → AI Configuration with API key, model selection, and temperature control.

**Database Health Dashboard** — Toggle the right-side panel to monitor connection health, server activity (active sessions, commits, rollbacks, cache hit ratio, lock waits, deadlocks), live connections, running queries with duration, SQL warnings, and JVM heap/thread stats. Auto-refreshes every 2 seconds.

**13 Live-Switching Themes** — Switch instantly from the toolbar dropdown. Flat Dark, Flat Darcula, Flat Light, Flat IntelliJ, Ocean Blue, Forest Green, Sunset Purple, Cherry Red, Amber Warm, Arctic Frost, Rose Garden, System, and Metal. Selection persists across restarts.

**Self-Update** — Check for new versions from the Help menu or About dialog. Downloads the latest JAR from GitHub Releases with progress tracking, optional SHA-256 verification, and automatic restart. Configurable startup check.

**Console Log** — Timestamped entries for every query executed, execution times, and errors with stack traces. Clear with one click.

**Status Bar** — Shows active connection details, real-time JVM heap usage with color-coded thresholds (green/amber/red), and a manual GC button.

## Quick Start

```
git clone https://github.com/adeptashishsriv/db-explorer.git
cd db-explorer
mvn clean package
java -jar target/db-explorer-3.0.1.jar
```

Requires Java 17+ and Maven 3.6+. Or download the pre-built JAR from GitHub Releases.

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Ctrl+Enter | Run query |
| Ctrl+Z / Ctrl+Y | Undo / Redo |
| Ctrl+Space | Auto-complete |
| Alt+S | Settings menu |
| Alt+H | Help menu |

## License

Copyright © 2026 Astro Adept AI Labs. All rights reserved.

For licensing inquiries: adeptashish@gmail.com
