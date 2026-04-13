# Implementation Plan: UI Test Robot

## Overview

Build a standalone `UITestRobot` main class in `com.dbexplorer.test` that uses `java.awt.Robot` to drive the DB Explorer Swing application through 20 sequential end-to-end test cases. The implementation is structured as: configuration parsing → helper utilities → test runner → individual test cases → reporting. All non-UI logic (config parsing, reporting, skip logic) is unit- and property-tested.

## Tasks

- [x] 1. Create TestConfig model and JSON parsing
  - [x] 1.1 Create `src/main/java/com/dbexplorer/test/UITestRobot.java` with the `TestConfig` inner record/class
    - Define fields: connectionName, databaseType, host, port, databaseName, username, password, query, ddlScriptPath, aiProvider, aiModel, aiBaseUrl, aiApiKey, aiPrompt
    - Implement `parseConfig(String filePath)` using Gson to deserialize JSON into TestConfig
    - Validate required fields (connectionName, databaseType, host, port, databaseName, username, password) and throw descriptive errors for missing fields
    - Handle invalid JSON with descriptive parse error messages
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 1.2 Create `TestResult` inner record and `shouldSkipAI()` helper
    - Define `TestResult(String name, Status status, long durationMs, String failureReason)` with `enum Status { PASS, FAIL, SKIP }`
    - Implement `shouldSkipAI(TestConfig config)` returning true when aiProvider or aiApiKey is null/empty
    - _Requirements: 17.1, 21.2_

  - [ ]* 1.3 Write property test: Config parsing round trip (Property 1)
    - **Property 1: Config parsing round trip**
    - **Validates: Requirements 1.2, 1.5, 21.1, 23.5**
    - Create `src/test/java/com/dbexplorer/test/UITestRobotPropertyTest.java`
    - Generate random TestConfig with all required fields and random optional fields, serialize to JSON via Gson, parse back, assert equality

  - [ ]* 1.4 Write property test: Invalid config rejection (Property 2)
    - **Property 2: Invalid config rejection**
    - **Validates: Requirements 1.4**
    - Generate invalid JSON strings and valid JSON missing required fields, assert parseConfig throws

  - [ ]* 1.5 Write property test: AI skip logic (Property 5)
    - **Property 5: AI test cases skip when AI config is absent**
    - **Validates: Requirements 21.2**
    - Generate TestConfig with/without aiProvider and aiApiKey, assert shouldSkipAI() returns correct boolean

- [x] 2. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 3. Implement TestReporter and reporting logic
  - [x] 3.1 Add `TestReporter` inner class to UITestRobot
    - Implement `reportPass(testName, durationMs)` logging `[PASS] testName (Xms)` to stdout
    - Implement `reportFail(testName, durationMs, reason)` logging `[FAIL] testName (Xms): reason` to stdout
    - Implement `reportSkip(testName, reason)` logging `[SKIP] testName: reason` to stdout
    - Implement `printSummary()` printing total/passed/failed/skipped counts
    - Implement `getExitCode()` returning 0 if failed==0, else 1
    - Store results in an internal `List<TestResult>`
    - _Requirements: 17.1, 17.2, 17.3, 17.4_

  - [ ]* 3.2 Write property test: Test report formatting (Property 3)
    - **Property 3: Test report formatting**
    - **Validates: Requirements 17.1, 17.2**
    - Generate random TestResult values, format report line, assert it contains name, status, duration; for FAIL assert it contains failure reason

  - [ ]* 3.3 Write property test: Test summary and exit code correctness (Property 4)
    - **Property 4: Test summary and exit code correctness**
    - **Validates: Requirements 17.3, 17.4**
    - Generate random lists of TestResult, compute summary counts, assert total/passed/failed/skipped match; assert exit code is 0 iff failed==0

- [x] 4. Implement RobotHelper utilities
  - [x] 4.1 Add RobotHelper methods to UITestRobot
    - Implement `delay(ms)` — Thread.sleep wrapper with default 500ms
    - Implement `findComponent(container, type, predicate)` — EDT-safe recursive search via `SwingUtilities.invokeAndWait`
    - Implement `findComponentByName(container, name)` — find by component name property
    - Implement `findDialog(titleSubstring, timeoutMs)` — poll `Window.getWindows()` for matching JDialog
    - Implement `findButton(container, textOrTooltip)` — find JButton by text or tooltip
    - Implement `clickComponent(component)` — move mouse to center, robot.mousePress/mouseRelease
    - Implement `clickButton(container, textOrTooltip)` — find + click
    - Implement `typeText(text)` — type string character by character via Robot key events
    - Implement `pressKey(keyCode)` and `pressCtrlKey(keyCode)` — key press helpers
    - Implement `waitForCondition(predicate, timeoutMs, description)` — poll at 100ms intervals
    - _Requirements: 18.1, 18.2, 18.3_

  - [x] 4.2 Add tree and menu helper methods
    - Implement `getTreeNodeByText(tree, text)` — find JTree node by user object toString
    - Implement `expandTreeNode(tree, path)` — expand a tree path on EDT
    - Implement `doubleClickTreeNode(tree, node)` — double-click a tree row
    - Implement `rightClickTreeNode(tree, node)` — right-click to trigger popup menu
    - Implement `clickMenuItem(popupMenu, text)` — find and click a JMenuItem
    - Implement `clickMenuBarItem(menuBar, menuText, itemText)` — navigate menu bar → menu → item
    - _Requirements: 18.1, 18.3_

- [x] 5. Implement test runner and main method
  - [x] 5.1 Implement the `main(String[] args)` entry point and test runner loop
    - Parse command-line arg for config file path; exit with code 1 and error message if missing or file not found
    - Parse TestConfig from JSON; exit with code 1 on parse error
    - Define the 20 test cases in order: launch, add-connection, test-connection, connect, browse-schema, open-query-tab, run-query, verify-results, create-sample-data, export-ddl, view-schema-diagram, configure-ai, ai-assistant, disconnect, about-dialog, help-dialog, switch-theme, check-updates, clear-console, delete-connection
    - Run each test case in a try-catch loop: on success call reportPass, on exception call reportFail, continue to next
    - After all tests: call printSummary, dispose MainFrame, System.exit with getExitCode()
    - _Requirements: 1.1, 1.3, 17.4, 17.5, 18.4, 18.5_

  - [ ]* 5.2 Write property test: Error isolation (Property 6)
    - **Property 6: Error isolation — runner continues after test failure**
    - **Validates: Requirements 18.4**
    - Generate sequence of test functions (some throwing), run through runner loop logic, assert all produce a TestResult

- [x] 6. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Implement test cases: launch and connection lifecycle
  - [x] 7.1 Implement `testLaunch()`
    - Create and display MainFrame on EDT via `SwingUtilities.invokeAndWait`
    - Wait up to 10 seconds for frame to be visible and showing
    - Store MainFrame reference for subsequent tests
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 7.2 Implement `testAddConnection()`
    - Click "Add Connection" toolbar button to open ConnectionDialog
    - Wait up to 5 seconds for dialog to appear
    - Populate connection name, database type combo, host, port, database, username, password from TestConfig
    - Click "Save" button
    - Verify connection appears in Schema_Tree
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

  - [x] 7.3 Implement `testTestConnection()`
    - Click "Add Connection" toolbar button to reopen ConnectionDialog
    - Populate fields from TestConfig
    - Click "Test Connection" button
    - Wait for success dialog ("Connection successful!") and click OK
    - Click "Cancel" to close dialog without saving duplicate
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

  - [x] 7.4 Implement `testConnect()`
    - Double-click the connection node matching TestConfig connectionName in Schema_Tree
    - Wait up to 15 seconds for schema child nodes to appear under the connection
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 8. Implement test cases: schema browsing and query execution
  - [x] 8.1 Implement `testBrowseSchema()`
    - Expand the connected database node
    - Expand the first schema node to reveal category folders
    - Expand the "Tables" category folder
    - Verify at least one table name appears
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [x] 8.2 Implement `testOpenQueryTab()`
    - Right-click the connected database node
    - Click "Open Query Tab" from context menu
    - Verify a new tab appears in SqlEditorPanel
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [x] 8.3 Implement `testRunQuery()`
    - Type SELECT statement into active query editor (from TestConfig.query or default `SELECT 1`)
    - Press Ctrl+Enter to execute
    - Wait up to 30 seconds for results to appear in the Results tab
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [x] 8.4 Implement `testVerifyResults()`
    - Check result table row count > 0
    - Verify at least one column header exists
    - Verify Console_Panel contains a log entry with query execution info
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

- [x] 9. Implement test cases: sample data, DDL export, schema diagram
  - [x] 9.1 Implement `testCreateSampleData()` with embedded supply chain DDL
    - Define the embedded DDL as a static String constant with CREATE TABLE and INSERT statements for 7 tables (suppliers, warehouses, products, inventory, purchase_orders, order_items, shipments) with FK relationships and 3+ rows each
    - If TestConfig.ddlScriptPath is set, read file contents instead
    - Open a new query tab, type and execute the DDL script
    - Verify no errors in Console_Panel
    - Execute `SELECT COUNT(*) FROM products` and verify count > 0
    - _Requirements: 23.1, 23.2, 23.3, 23.4, 23.5, 23.6, 23.7_

  - [x] 9.2 Implement `testExportDdl()`
    - Right-click the first schema node in Schema_Tree
    - Click "Export DDL" from context menu
    - Wait up to 10 seconds for DdlExportDialog
    - Verify dialog contains DDL text with CREATE TABLE
    - Click "Copy" button, verify clipboard contains text starting with "CREATE"
    - Close dialog
    - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5, 19.6, 19.7_

  - [x] 9.3 Implement `testViewSchemaDiagram()`
    - Right-click the first schema node in Schema_Tree
    - Click "Schema Diagram" from context menu
    - Wait up to 10 seconds for SchemaDiagramDialog
    - Verify dialog contains a canvas with table boxes (SchemaDiagramPanel)
    - Click "Fit" button, wait 2 seconds for render
    - Close dialog
    - _Requirements: 20.1, 20.2, 20.3, 20.4, 20.5, 20.6, 20.7_

- [x] 10. Implement test cases: AI, disconnect, dialogs, theme, updates, cleanup
  - [x] 10.1 Implement `testConfigureAI()`
    - Skip with SKIP status if shouldSkipAI(config) returns true
    - Open Settings menu → "AI Configuration..."
    - Wait up to 5 seconds for AIConfigDialog
    - Click "Add New", populate config name, provider, model, base URL, API key from TestConfig
    - Click "Save", dismiss confirmation dialog, close AIConfigDialog
    - _Requirements: 21.1, 21.2, 21.3, 21.4, 21.5, 21.6, 21.7, 21.8, 21.9_

  - [x] 10.2 Implement `testAIAssistant()`
    - Skip with SKIP status if configure-ai was skipped
    - Click AI Assistant toolbar button (🤖)
    - Wait up to 5 seconds for AI_Assistant_Dialog
    - Type prompt from TestConfig.aiPrompt or default "Show all tables in the database"
    - Click "Generate SQL" button
    - Wait up to 30 seconds for generated SQL to appear
    - Verify output text area is not empty
    - Close dialog
    - _Requirements: 22.1, 22.2, 22.3, 22.4, 22.5, 22.6, 22.7, 22.8, 22.9_

  - [x] 10.3 Implement `testDisconnect()`
    - Click "Disconnect DB" toolbar button
    - Click "Yes" on confirmation dialog
    - Verify status bar no longer shows active connection
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

  - [x] 10.4 Implement `testAboutDialog()`
    - Click "About" toolbar button
    - Wait up to 5 seconds for AboutDialog
    - Verify "Version" label, "OK" button, and "Check for Updates…" button are present
    - Click "OK" to close
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

  - [x] 10.5 Implement `testHelpDialog()`
    - Open Help menu → "User Guide"
    - Wait up to 5 seconds for HelpDialog
    - Verify tabs: "User Guide", "DB Connection", "AI Configuration", "DB Health", "Execution Plan", "Themes"
    - Click each tab in sequence
    - Click "Close" to dismiss
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

  - [x] 10.6 Implement `testSwitchTheme()`
    - Locate Theme_Combo in toolbar
    - Record initial theme, select a different theme (e.g. "Flat Light")
    - Verify UIManager.getLookAndFeel() class name changed
    - Restore original theme
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_

  - [x] 10.7 Implement `testCheckUpdates()`
    - Open Help menu → "Check for Updates…"
    - Wait up to 5 seconds for UpdateDialog
    - Verify dialog is visible with title containing "Update"
    - Click "Close" to dismiss
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5_

  - [x] 10.8 Implement `testClearConsole()`
    - Click "Clear Console" toolbar button
    - Verify Console_Panel text area is empty within 2 seconds
    - _Requirements: 15.1, 15.2, 15.3_

  - [x] 10.9 Implement `testDeleteConnection()`
    - Right-click the test connection node in Schema_Tree
    - Click "Delete" from context menu
    - Click "Yes" on confirmation dialog
    - Verify connection no longer appears in Schema_Tree
    - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5_

- [x] 11. Create example test configuration file
  - Create `src/test/resources/test-config-example.json` with a sample TestConfig JSON showing all fields (required and optional) with placeholder values
  - _Requirements: 1.2, 1.5, 21.1_

- [x] 12. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The UITestRobot class lives in `src/main/java` (not test) since it's a standalone main class meant to be run independently via `java -cp db-explorer.jar com.dbexplorer.test.UITestRobot test-config.json`
- Property tests live in `src/test/java` and use jqwik (already in pom.xml)
