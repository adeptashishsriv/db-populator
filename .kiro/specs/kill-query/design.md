# Design Document — Cancel Running Query

## Overview

This feature adds a "Cancel Running Query" stop button to the MainFrame toolbar. When a
query is in-flight, the button is enabled; clicking it calls `Statement.cancel()` on the
active JDBC statement via a new `cancelCurrent()` method on `QueryExecutor`. After
cancellation the ResultPanel shows "Query cancelled".

The design is deliberately minimal:

- `QueryExecutor` gains an `AtomicReference<Statement>` and a `cancelCurrent()` method.
- `DbIcons` gains a `TB_CANCEL` toolbar icon (red circle + white square).
- `MainFrame` gains a `cancelBtn` field, wires it into `initToolbar()`, and modifies
  `runQuery()` to enable/disable it around the `executeAsync` call.
- No new classes, no new connections, no new threads.

---

## Architecture

```
MainFrame toolbar
  │
  ├─ cancelBtn (JButton, TB_CANCEL icon) — disabled by default
  │
  ├─ runQuery()
  │     → cancelBtn.setEnabled(true)
  │     → queryExecutor.executeAsync(conn, sql, ...)  [Future stored]
  │
  ├─ on query complete / error (invokeLater)
  │     → cancelBtn.setEnabled(false)
  │
  └─ cancelBtn.actionListener → cancelQuery()
        → queryExecutor.cancelCurrent()   [calls stmt.cancel() on background thread]
        → cancelBtn.setEnabled(false)
        → logPanel.logInfo("Cancel requested.")

QueryExecutor (extended)
  │
  ├─ activeStatement : AtomicReference<Statement>
  │     set before stmt.execute(sql)
  │     cleared in finally block
  │
  └─ cancelCurrent() : void
        stmt = activeStatement.get()
        if stmt != null → stmt.cancel()  [SQLException silently ignored]
```

### Threading model

| Thread | Responsibility |
|--------|---------------|
| EDT | Enable/disable cancel button; show "Query cancelled" in ResultPanel |
| `query-exec` (existing pool) | Execute `executeAsync()`; sets/clears `activeStatement` |

`cancelCurrent()` may be called from the EDT (via the button's action listener). The
`AtomicReference` makes the cross-thread access safe without locking.

---

## Components

### `QueryExecutor` changes

New field:

```java
private final AtomicReference<Statement> activeStatement = new AtomicReference<>();
```

Modified `executeAsync()` — set/clear `activeStatement` around `stmt.execute()`:

```java
public Future<?> executeAsync(Connection connection, String sql, ...) {
    return executor.submit(() -> {
        Statement stmt = null;
        try {
            long start = System.currentTimeMillis();
            stmt = connection.createStatement(
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(LazyQueryResult.DEFAULT_FETCH_SIZE);
            stmt.setFetchDirection(ResultSet.FETCH_FORWARD);
            activeStatement.set(stmt);          // <-- new
            boolean hasResultSet = stmt.execute(sql);
            activeStatement.set(null);          // <-- new (clear on success path)
            ...
        } catch (SQLException e) {
            onError.accept(e);
        } finally {
            activeStatement.set(null);          // <-- new (guarantee clear)
        }
    });
}
```

New method:

```java
/**
 * Cancels the currently executing JDBC Statement, if any.
 * Safe to call from any thread. No-op if no statement is active.
 */
public void cancelCurrent() {
    Statement stmt = activeStatement.get();
    if (stmt != null) {
        try { stmt.cancel(); } catch (SQLException ignored) {}
    }
}
```

### `DbIcons` changes

New constant:

```java
public static final Icon TB_CANCEL = px(TB, DbIcons::tbCancel);
```

New painter (red circle + white filled square — universal stop symbol):

```java
static void tbCancel(Graphics2D g, int s) {
    g.setColor(C_RED);
    g.fillOval(1, 1, s-2, s-2);
    g.setColor(C_WHITE);
    int sq = s / 3;
    g.fillRect(s/2 - sq/2, s/2 - sq/2, sq, sq);
}
```

### `MainFrame` changes

New fields:

```java
private JButton cancelBtn;
```

`initToolbar()` — add `cancelBtn` immediately after `runBtn`:

```java
cancelBtn = makeToolButton("Cancel Running Query", DbIcons.TB_CANCEL);
cancelBtn.setEnabled(false);
cancelBtn.addActionListener(e -> cancelQuery());
toolbar.add(cancelBtn);   // inserted right after toolbar.add(runBtn)
```

`runQuery()` — enable cancel button before `executeAsync`, disable in all callbacks:

```java
// JDBC path only — before executeAsync:
cancelBtn.setEnabled(true);
queryExecutor.executeAsync(conn, sql,
    (LazyQueryResult lazyResult) -> SwingUtilities.invokeLater(() -> {
        cancelBtn.setEnabled(false);
        rp.hideLoading();
        rp.displayLazyResult(lazyResult);
        ...
    }),
    (QueryResult result) -> SwingUtilities.invokeLater(() -> {
        cancelBtn.setEnabled(false);
        rp.hideLoading();
        rp.displayResult(result);
        ...
    }),
    (SQLException ex) -> SwingUtilities.invokeLater(() -> {
        cancelBtn.setEnabled(false);
        rp.hideLoading();
        if (isCancelException(ex)) {
            rp.displayError("Query cancelled", "The query was cancelled by the user.");
        } else {
            rp.displayError("SQL Error [" + ex.getErrorCode() + "]", ex.getMessage());
        }
        ...
    })
);
```

New methods in `MainFrame`:

```java
private void cancelQuery() {
    queryExecutor.cancelCurrent();
    cancelBtn.setEnabled(false);
    logPanel.logInfo("Cancel requested.");
}

private static boolean isCancelException(SQLException ex) {
    String state = ex.getSQLState();
    String msg   = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
    return "57014".equals(state) || msg.contains("cancel") || msg.contains("interrupt");
}
```

---

## Correctness Properties

### Property 1: Cancel button enabled state mirrors query in-flight state

For any point in time, `cancelBtn` must be enabled if and only if a JDBC query is currently
executing. Specifically: disabled before any query runs, enabled immediately when
`runQuery()` calls `executeAsync()`, and disabled again when the query completes, errors,
or is cancelled.

**Validates: Requirements 1.2, 1.3, 1.4, 1.5, 1.6, 1.7**

---

### Property 2: `cancelCurrent()` invokes `Statement.cancel()` on the active statement

For any `QueryExecutor` instance with an in-flight `executeAsync` call, invoking
`cancelCurrent()` must call `Statement.cancel()` on the statement tracked in
`activeStatement`. If no statement is active, `cancelCurrent()` must be a no-op and must
not throw.

**Validates: Requirements 2.1, 2.3, 4.4, 4.5**

---

### Property 3: `activeStatement` is always cleared after `executeAsync`

For any `executeAsync` call — whether it succeeds, throws a `SQLException`, or is
cancelled — `activeStatement.get()` must return `null` after the submitted task completes.

**Validates: Requirements 4.2, 4.3**

---

### Property 4: `isCancelException` correctly classifies SQLExceptions

For any `SQLException` with `SQLState "57014"`, or whose message contains "cancel" or
"interrupt" (case-insensitive), `isCancelException` must return `true`. For any
`SQLException` that does not meet these criteria, it must return `false`.

**Validates: Requirement 3.1**

---

## Error Handling

| Scenario | Handling |
|----------|----------|
| `Statement.cancel()` throws `SQLException` | Caught inside `cancelCurrent()`; silently ignored (best-effort) |
| `cancelCurrent()` called with no active statement | `activeStatement.get()` returns `null`; method is a no-op |
| JDBC driver signals cancellation via `SQLException` | `isCancelException()` detects it; ResultPanel shows "Query cancelled" |
| Cancel button clicked after query already finished | `cancelCurrent()` is a no-op; button is already disabled |
| DynamoDB path — cancel button clicked | DynamoDB path does not enable `cancelBtn`; button stays disabled |

---

## Testing Strategy

### Unit tests (`QueryExecutorCancelTest`)

- `cancelCurrent()` with no active statement is a no-op (no exception thrown)
- `cancelCurrent()` calls `Statement.cancel()` on a mock statement set in `activeStatement`
- `activeStatement` is `null` after `executeAsync` completes normally
- `activeStatement` is `null` after `executeAsync` encounters a `SQLException`
- `isCancelException` returns `true` for `SQLState "57014"`
- `isCancelException` returns `true` for message containing "cancel" (case-insensitive)
- `isCancelException` returns `true` for message containing "interrupt"
- `isCancelException` returns `false` for an unrelated `SQLException`

### Property-based tests (`QueryExecutorCancelPropertyTest`)

Uses **jqwik 1.8.4** (already in `pom.xml`). Each test runs a minimum of **100 tries**.

| Test method | Design property |
|---|---|
| `cancelButtonMirrorsInFlightState` | Property 1 |
| `cancelCurrentInvokesStatementCancel` | Property 2 |
| `activeStatementAlwaysCleared` | Property 3 |
| `isCancelExceptionClassification` | Property 4 |

### Manual tests

- Cancel button is disabled on application start
- Cancel button becomes enabled immediately when Run Query is clicked
- Clicking cancel during a long-running query shows "Query cancelled" in the Results panel
- Cancel button returns to disabled after cancellation
- Cancel button returns to disabled after normal query completion
- `TB_CANCEL` icon renders correctly at 20×20 px across all themes
