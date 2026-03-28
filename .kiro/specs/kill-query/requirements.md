# Requirements Document

## Introduction

The Cancel Running Query feature adds a stop button to the SQL query editor toolbar in
MainFrame. When a user runs a query that takes a long time, they can click this button to
cancel it. The cancellation is performed via the JDBC `Statement.cancel()` API on the
in-flight statement. After cancellation, the ResultPanel shows "Query cancelled". The button
is disabled when no query is running and becomes enabled when a query starts.

---

## Glossary

- **Cancel Button**: The toolbar `JButton` with a stop icon that triggers query cancellation.
- **Active Statement**: The `java.sql.Statement` currently executing inside `QueryExecutor.executeAsync()`.
- **Cancel Exception**: A `SQLException` thrown by the JDBC driver in response to `Statement.cancel()`, typically identified by `SQLState "57014"` or a message containing "cancel" or "interrupt".
- **QueryExecutor**: The existing `com.dbexplorer.service.QueryExecutor` class that executes SQL asynchronously.
- **MainFrame**: The main application window containing the toolbar and query editor.
- **ResultPanel**: The panel that displays query results or error messages for the active tab.

---

## Requirements

### Requirement 1: Cancel Button in Toolbar

**User Story:** As a developer, I want a cancel button in the toolbar so that I can stop a
long-running query without closing the application.

#### Acceptance Criteria

1. THE MainFrame toolbar SHALL contain a "Cancel Running Query" button with a stop icon,
   positioned immediately after the Run Query button.
2. THE cancel button SHALL be disabled when the application starts.
3. THE cancel button SHALL be disabled when no query is currently executing.
4. WHEN a query begins executing (i.e. `runQuery()` submits work to `QueryExecutor`), THE
   cancel button SHALL become enabled.
5. WHEN a query completes successfully, THE cancel button SHALL become disabled.
6. WHEN a query completes with an error, THE cancel button SHALL become disabled.
7. WHEN a query is cancelled, THE cancel button SHALL become disabled.

---

### Requirement 2: Query Cancellation via JDBC

**User Story:** As a developer, I want clicking the cancel button to interrupt the running
query so that I am not blocked waiting for it to finish.

#### Acceptance Criteria

1. WHEN the user clicks the cancel button, THE application SHALL call
   `Statement.cancel()` on the currently executing JDBC statement.
2. THE cancellation SHALL be performed without opening a new JDBC connection.
3. IF no query is currently executing when the cancel button is clicked, THE action SHALL
   be a no-op and SHALL NOT throw an exception.
4. THE `Statement.cancel()` call SHALL be made from a non-EDT thread (the query executor
   thread or any background thread); the EDT SHALL NOT block waiting for it.
5. IF `Statement.cancel()` throws a `SQLException`, THE exception SHALL be silently
   ignored (best-effort cancellation).

---

### Requirement 3: Post-Cancellation Feedback

**User Story:** As a developer, I want to see a clear message when my query is cancelled so
that I know the cancellation worked.

#### Acceptance Criteria

1. WHEN the JDBC driver signals cancellation by throwing a `SQLException` with
   `SQLState "57014"` or a message containing "cancel" or "interrupt" (case-insensitive),
   THE ResultPanel SHALL display "Query cancelled" instead of a generic SQL error message.
2. THE "Query cancelled" message SHALL be shown in the ResultPanel of the active tab.
3. WHEN a query is cancelled, THE loading indicator in the ResultPanel SHALL be hidden.

---

### Requirement 4: QueryExecutor Cancel Support

**User Story:** As a developer, I want the QueryExecutor to expose a cancel mechanism so
that MainFrame can trigger cancellation without tight coupling to JDBC internals.

#### Acceptance Criteria

1. THE `QueryExecutor` class SHALL maintain an `AtomicReference<Statement>` that holds the
   currently executing statement, or `null` when no statement is active.
2. THE `AtomicReference` SHALL be set to the active `Statement` before `stmt.execute(sql)`
   is called inside `executeAsync()`.
3. THE `AtomicReference` SHALL be cleared to `null` after the statement completes,
   regardless of whether it succeeded or threw an exception (i.e. in a `finally` block).
4. THE `QueryExecutor` SHALL expose a `cancelCurrent()` method that retrieves the active
   statement and calls `Statement.cancel()` on it.
5. IF `cancelCurrent()` is called when no statement is active, it SHALL be a no-op.

---

### Requirement 5: Stop Icon

**User Story:** As a developer, I want the cancel button to have a recognizable stop icon
so that its purpose is immediately clear.

#### Acceptance Criteria

1. THE `DbIcons` class SHALL provide a `TB_CANCEL` toolbar icon.
2. THE icon SHALL use a red circle with a white filled square (universal stop symbol),
   consistent with the existing toolbar icon style.
3. THE icon SHALL be rendered at 20×20 pixels, matching all other toolbar icons.
