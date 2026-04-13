# Implementation Plan: Cancel Running Query

## Overview

Add a stop button to the MainFrame toolbar that cancels an in-flight JDBC query via
`Statement.cancel()`. Three touch points: `QueryExecutor` (expose cancel), `DbIcons` (new
icon), and `MainFrame` (button + wiring).

## Tasks

- [x] 1. Extend `QueryExecutor` with cancel support
  - [x] 1.1 Add `AtomicReference<Statement> activeStatement` field to `QueryExecutor`
    - Import `java.util.concurrent.atomic.AtomicReference`
    - Set `activeStatement` to the new `stmt` before `stmt.execute(sql)` in `executeAsync`
    - Clear `activeStatement` to `null` in a `finally` block to guarantee cleanup on both
      success and error paths
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 1.2 Add `cancelCurrent()` method to `QueryExecutor`
    - Retrieve `activeStatement.get()`; if non-null call `stmt.cancel()` catching
      `SQLException` silently
    - If null, method is a no-op
    - _Requirements: 4.4, 4.5_

  - [ ]* 1.3 Write property test for `QueryExecutor` — Property 2: `cancelCurrent()` invokes `Statement.cancel()`
    - **Property 2: `cancelCurrent()` invokes `Statement.cancel()` on the active statement**
    - **Validates: Requirements 2.1, 2.3, 4.4, 4.5**

  - [ ]* 1.4 Write property test for `QueryExecutor` — Property 3: `activeStatement` is always cleared
    - **Property 3: `activeStatement` is always cleared after `executeAsync`**
    - **Validates: Requirements 4.2, 4.3**

  - [ ]* 1.5 Write unit tests for `QueryExecutor` cancel in `QueryExecutorCancelTest`
    - `cancelCurrent()` with no active statement is a no-op (no exception thrown)
    - `cancelCurrent()` calls `Statement.cancel()` on a mock statement set in `activeStatement`
    - `activeStatement` is `null` after `executeAsync` completes normally
    - `activeStatement` is `null` after `executeAsync` encounters a `SQLException`
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 2. Checkpoint — Ensure all `QueryExecutor` tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 3. Add `TB_CANCEL` icon to `DbIcons`
  - Add `public static final Icon TB_CANCEL = px(TB, DbIcons::tbCancel);` constant in the
    toolbar icons section
  - Add `static void tbCancel(Graphics2D g, int s)` painter: red circle + white filled
    square (stop symbol), following the same pattern as `tbDisconnect` and `tbRun`
  - _Requirements: 5.1, 5.2, 5.3_

- [x] 4. Add `cancelBtn` to `MainFrame` toolbar and wire up cancellation
  - [x] 4.1 Add `cancelBtn` field to `MainFrame` and wire into `initToolbar()`
    - Add `private JButton cancelBtn;` field
    - In `initToolbar()`, create button with `DbIcons.TB_CANCEL`, tooltip "Cancel Running
      Query", disabled by default
    - Add `cancelBtn.addActionListener(e -> cancelQuery())`
    - Insert `cancelBtn` into the toolbar immediately after `runBtn`
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 4.2 Modify `runQuery()` in `MainFrame` to manage cancel button state
    - In the JDBC path only: call `cancelBtn.setEnabled(true)` before `executeAsync`
    - In the `onLazyResult` callback: call `cancelBtn.setEnabled(false)`
    - In the `onSuccess` callback: call `cancelBtn.setEnabled(false)`
    - In the `onError` callback: call `cancelBtn.setEnabled(false)`; detect cancellation
      via `isCancelException(ex)` and show "Query cancelled" message instead of generic
      SQL error
    - _Requirements: 1.4, 1.5, 1.6, 1.7, 3.1, 3.2, 3.3_

  - [x] 4.3 Add `cancelQuery()` and `isCancelException()` methods to `MainFrame`
    - `cancelQuery()`: calls `queryExecutor.cancelCurrent()`, sets
      `cancelBtn.setEnabled(false)`, logs "Cancel requested."
    - `isCancelException(SQLException)`: returns `true` if `SQLState == "57014"` or
      message contains "cancel" or "interrupt" (case-insensitive)
    - _Requirements: 2.1, 2.2, 2.3, 3.1_

  - [ ]* 4.4 Write property test for `isCancelException` — Property 4: correct classification
    - **Property 4: `isCancelException` correctly classifies SQLExceptions**
    - **Validates: Requirement 3.1**

  - [ ]* 4.5 Write unit tests for `MainFrame` cancel logic in `MainFrameCancelTest`
    - `isCancelException` returns `true` for `SQLState "57014"`
    - `isCancelException` returns `true` for message containing "cancel" (case-insensitive)
    - `isCancelException` returns `true` for message containing "interrupt"
    - `isCancelException` returns `false` for an unrelated `SQLException`
    - _Requirements: 3.1_

- [x] 5. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Property tests use jqwik 1.8.4 (already in `pom.xml`) with `@Property(tries=100)`
- The DynamoDB execution path in `runQuery()` does NOT enable `cancelBtn` — DynamoDB uses
  its own executor and does not go through `QueryExecutor`
- `cancelCurrent()` is safe to call from the EDT; `AtomicReference` handles cross-thread
  visibility without locking
