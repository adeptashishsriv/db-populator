# Requirements Document

## Introduction

An automated UI testing robot for DB Explorer that drives the real Swing application like a human end user. The robot uses `java.awt.Robot` to simulate mouse clicks, keyboard input, and menu navigation. It runs in the same JVM as DB Explorer by launching `MainFrame` programmatically, reads database connection properties from a JSON configuration file, and executes a sequence of foundational end-to-end test cases. Each test case logs a PASS/FAIL result. The robot is a standalone main class (`com.dbexplorer.test.UITestRobot`) that can be run independently.

## Glossary

- **Robot**: The `java.awt.Robot`-based test automation driver that simulates user input (mouse clicks, key presses, mouse movement)
- **MainFrame**: The primary `JFrame` window of DB Explorer (`com.dbexplorer.ui.MainFrame`)
- **Test_Config**: A JSON file containing database connection properties (host, port, username, password, database name, database type) used by the Robot
- **Test_Runner**: The main orchestrator class (`com.dbexplorer.test.UITestRobot`) that loads configuration, launches the application, and executes test cases in sequence
- **Test_Case**: A single named test scenario that performs UI actions and verifies expected outcomes
- **Test_Report**: The structured log output recording each Test_Case name, status (PASS/FAIL), duration, and failure reason if applicable
- **Connection_Dialog**: The `ConnectionDialog` modal used to add or edit database connections
- **Schema_Tree**: The `JTree` in `ConnectionListPanel` showing connections, schemas, tables, views, and other database objects
- **Query_Tab**: A tab in `SqlEditorPanel` containing a SQL editor and result panel bound to a specific connection
- **Console_Panel**: The `LogPanel` at the bottom of MainFrame displaying execution logs
- **About_Dialog**: The `AboutDialog` modal showing version info, OK button, and Check for Updates button
- **Help_Dialog**: The `HelpDialog` modal with tabbed help content (User Guide, DB Connection, AI Configuration, DB Health, Execution Plan, Themes)
- **Theme_Combo**: The `JComboBox` in the toolbar for switching UI themes
- **DDL_Export_Dialog**: The `DdlExportDialog` showing generated DDL for a schema
- **Schema_Diagram_Dialog**: The `SchemaDiagramDialog` showing the ER diagram
- **AI_Config_Dialog**: The `AIConfigDialog` for configuring AI model settings
- **AI_Assistant_Dialog**: The AI Assistant dialog for natural language SQL generation

## Requirements

### Requirement 1: Test Configuration Loading

**User Story:** As a tester, I want the robot to read database connection properties from a JSON configuration file, so that I can run tests against different databases without modifying code.

#### Acceptance Criteria

1. WHEN the Test_Runner starts, THE Test_Runner SHALL read the Test_Config file path from the first command-line argument
2. THE Test_Config SHALL contain the following properties: connection name, database type (matching `DatabaseType` enum values: POSTGRESQL, MYSQL, ORACLE, SQLSERVER, SQLITE, DYNAMODB, GENERIC), host, port, database name, username, and password
3. IF the Test_Config file path is missing or the file does not exist, THEN THE Test_Runner SHALL log an error message and exit with a non-zero exit code
4. IF the Test_Config file contains invalid JSON or missing required fields, THEN THE Test_Runner SHALL log a descriptive parse error and exit with a non-zero exit code
5. WHEN the Test_Config is loaded, THE Test_Runner SHALL also accept an optional `query` field specifying the SELECT statement to execute during the query test case

### Requirement 2: Application Launch

**User Story:** As a tester, I want the robot to launch DB Explorer programmatically within the same JVM, so that the robot has direct access to Swing components for reliable automation.

#### Acceptance Criteria

1. WHEN the Test_Runner executes the launch test case, THE Test_Runner SHALL create and display the MainFrame on the Event Dispatch Thread using `SwingUtilities.invokeAndWait`
2. WHEN the MainFrame becomes visible, THE Robot SHALL wait until the frame is showing and ready for input before proceeding
3. IF the MainFrame fails to become visible within 10 seconds, THEN THE Test_Runner SHALL report the launch test case as FAIL
4. THE Test_Runner SHALL store a reference to the MainFrame instance for use by subsequent test cases

### Requirement 3: Add New Database Connection

**User Story:** As a tester, I want the robot to add a new database connection using properties from the configuration file, so that subsequent tests can operate against a real database.

#### Acceptance Criteria

1. WHEN the add-connection test case executes, THE Robot SHALL click the "Add Connection" toolbar button to open the Connection_Dialog
2. WHEN the Connection_Dialog is visible, THE Robot SHALL populate the connection name, database type, host, port, database name, username, and password fields from the Test_Config
3. WHEN the database type is selected, THE Robot SHALL select the matching `DatabaseType` value from the type combo box
4. WHEN all fields are populated, THE Robot SHALL click the "Save" button on the Connection_Dialog
5. WHEN the Connection_Dialog closes, THE Test_Runner SHALL verify that the new connection appears in the Schema_Tree
6. IF the Connection_Dialog does not open within 5 seconds, THEN THE Test_Runner SHALL report the test case as FAIL

### Requirement 4: Test Connection

**User Story:** As a tester, I want the robot to test the database connection, so that I can verify connectivity before running further tests.

#### Acceptance Criteria

1. WHEN the test-connection test case executes, THE Robot SHALL click the "Add Connection" toolbar button to open the Connection_Dialog
2. WHEN the Connection_Dialog is visible with fields populated, THE Robot SHALL click the "Test Connection" button
3. WHEN a success dialog appears with the message "Connection successful!", THE Robot SHALL click OK to dismiss the dialog
4. THE Test_Runner SHALL report the test case as PASS when the success dialog is detected
5. IF an error dialog appears instead of the success dialog, THEN THE Test_Runner SHALL report the test case as FAIL with the error message
6. WHEN the test is complete, THE Robot SHALL click "Cancel" to close the Connection_Dialog without saving a duplicate

### Requirement 5: Connect to Database

**User Story:** As a tester, I want the robot to connect to the saved database connection, so that schema browsing and query execution can proceed.

#### Acceptance Criteria

1. WHEN the connect test case executes, THE Robot SHALL double-click the connection node in the Schema_Tree that matches the Test_Config connection name
2. WHEN the connection succeeds, THE Test_Runner SHALL verify that the connection node in the Schema_Tree shows a connected state (child schema nodes appear)
3. IF the connection fails and an error dialog appears, THEN THE Test_Runner SHALL report the test case as FAIL with the error message
4. THE Test_Runner SHALL wait up to 15 seconds for schema nodes to load under the connection node before reporting FAIL

### Requirement 6: Browse Schema Tree

**User Story:** As a tester, I want the robot to expand and browse the schema tree, so that I can verify the schema explorer loads database objects correctly.

#### Acceptance Criteria

1. WHEN the browse-schema test case executes, THE Robot SHALL expand the connected database node in the Schema_Tree
2. WHEN schema nodes are visible, THE Robot SHALL expand the first schema node to reveal category folders (Tables, Views, Sequences, Indexes, Functions, Procedures)
3. WHEN category folders are visible, THE Robot SHALL expand the "Tables" category folder
4. THE Test_Runner SHALL verify that at least one table name appears under the Tables folder
5. IF no schema nodes or table nodes load within 10 seconds, THEN THE Test_Runner SHALL report the test case as FAIL

### Requirement 7: Open Query Tab

**User Story:** As a tester, I want the robot to open a new query tab bound to the test connection, so that SQL queries can be executed.

#### Acceptance Criteria

1. WHEN the open-query-tab test case executes, THE Robot SHALL right-click the connected database node in the Schema_Tree
2. WHEN the context menu appears, THE Robot SHALL click the "Open Query Tab" menu item
3. THE Test_Runner SHALL verify that a new tab appears in the SqlEditorPanel with the connection name in the tab title
4. IF the context menu does not appear within 3 seconds, THEN THE Test_Runner SHALL report the test case as FAIL

### Requirement 8: Run SELECT Query

**User Story:** As a tester, I want the robot to execute a SELECT query and verify results appear, so that I can confirm the query execution pipeline works end-to-end.

#### Acceptance Criteria

1. WHEN the run-query test case executes, THE Robot SHALL type a SELECT statement into the active query editor (using the `query` field from Test_Config, or defaulting to `SELECT 1`)
2. WHEN the SQL text is entered, THE Robot SHALL press Ctrl+Enter to execute the query
3. THE Test_Runner SHALL verify that the Results tab in the active Query_Tab displays a result table with at least one row
4. THE Test_Runner SHALL wait up to 30 seconds for query results to appear before reporting FAIL
5. IF an error message appears in the result panel, THEN THE Test_Runner SHALL report the test case as FAIL with the error text

### Requirement 9: Verify Query Results

**User Story:** As a tester, I want the robot to verify that query results are displayed correctly, so that I can confirm the result rendering pipeline works.

#### Acceptance Criteria

1. WHEN the verify-results test case executes, THE Test_Runner SHALL check that the result table in the active Query_Tab has a row count greater than zero
2. THE Test_Runner SHALL verify that the result table has at least one column header
3. THE Test_Runner SHALL verify that the Console_Panel contains a log entry with the query execution time
4. IF the result table is empty or not visible, THEN THE Test_Runner SHALL report the test case as FAIL

### Requirement 23: Create Supply Chain Sample Data Model

**User Story:** As a tester, I want the robot to create a supply chain sample data model with related tables and insert sample records, so that subsequent tests (schema diagram, DDL export, query) operate on meaningful data.

#### Acceptance Criteria

1. WHEN the create-sample-data test case executes, THE Robot SHALL open a new query tab bound to the test connection
2. THE Robot SHALL type and execute a DDL script that creates the following supply chain tables with foreign key relationships: `suppliers` (supplier_id PK, name, contact_email, country, rating), `warehouses` (warehouse_id PK, name, location, capacity), `products` (product_id PK, name, sku, category, unit_price, supplier_id FK → suppliers), `inventory` (inventory_id PK, product_id FK → products, warehouse_id FK → warehouses, quantity, last_restocked), `purchase_orders` (order_id PK, supplier_id FK → suppliers, order_date, status, total_amount), `order_items` (item_id PK, order_id FK → purchase_orders, product_id FK → products, quantity, unit_price), `shipments` (shipment_id PK, order_id FK → purchase_orders, warehouse_id FK → warehouses, ship_date, arrival_date, tracking_number, status)
3. THE Robot SHALL execute INSERT statements to populate each table with at least 3 sample records
4. THE Robot SHALL verify that each CREATE TABLE and INSERT statement executes without error by checking the Console_Panel for error entries
5. THE Test_Config SHALL support an optional `ddl_script_path` field pointing to a custom SQL file; if present, the Robot SHALL execute that file's contents instead of the built-in supply chain DDL
6. IF any DDL or INSERT statement fails, THEN THE Test_Runner SHALL report the test case as FAIL with the error message
7. WHEN all DDL and INSERT statements succeed, THE Robot SHALL execute `SELECT COUNT(*) FROM products` and verify the count is greater than zero

### Requirement 19: Export Schema DDL

**User Story:** As a tester, I want the robot to export DDL for a schema, so that I can verify the DDL export feature generates valid CREATE TABLE statements and supports clipboard copy.

#### Acceptance Criteria

1. WHEN the export-ddl test case executes, THE Robot SHALL right-click the first schema node in the Schema_Tree
2. WHEN the context menu appears, THE Robot SHALL click the "Export DDL" menu item
3. WHEN the DDL_Export_Dialog is visible, THE Test_Runner SHALL verify that the dialog contains DDL text including CREATE TABLE statements
4. THE Robot SHALL click the "Copy" button in the DDL_Export_Dialog to copy the DDL to the clipboard
5. THE Test_Runner SHALL verify that the system clipboard contains text starting with "CREATE"
6. WHEN verification is complete, THE Robot SHALL close the DDL_Export_Dialog
7. IF the DDL_Export_Dialog does not open within 10 seconds, THEN THE Test_Runner SHALL report the test case as FAIL

### Requirement 20: View Schema Diagram

**User Story:** As a tester, I want the robot to open the schema diagram, so that I can verify the ER diagram renders table boxes and supports fit-to-view.

#### Acceptance Criteria

1. WHEN the view-schema-diagram test case executes, THE Robot SHALL right-click the first schema node in the Schema_Tree
2. WHEN the context menu appears, THE Robot SHALL click the "Schema Diagram" menu item
3. WHEN the Schema_Diagram_Dialog is visible, THE Test_Runner SHALL verify that the dialog contains a canvas showing table boxes
4. THE Robot SHALL click the "Fit" button in the Schema_Diagram_Dialog to fit all tables into view
5. THE Robot SHALL wait 2 seconds for the diagram to render after clicking "Fit"
6. WHEN verification is complete, THE Robot SHALL close the Schema_Diagram_Dialog
7. IF the Schema_Diagram_Dialog does not open within 10 seconds, THEN THE Test_Runner SHALL report the test case as FAIL

### Requirement 21: Configure AI Model

**User Story:** As a tester, I want the robot to configure an AI model from optional Test_Config fields, so that I can verify the AI configuration dialog works and enable AI-dependent test cases.

#### Acceptance Criteria

1. THE Test_Config SHALL support optional AI configuration fields: `ai_provider` (e.g. "OpenAI"), `ai_model` (e.g. "gpt-4o"), `ai_base_url`, and `ai_api_key`
2. IF the Test_Config does not contain the `ai_provider` or `ai_api_key` fields, THEN THE Test_Runner SHALL skip the configure-ai test case with status SKIP
3. WHEN the configure-ai test case executes, THE Robot SHALL open the Settings menu and click "AI Configuration..."
4. WHEN the AI_Config_Dialog is visible, THE Robot SHALL click the "Add New" button to create a new configuration
5. THE Robot SHALL populate the config name, provider, model, base URL, and API key fields from the Test_Config AI configuration fields
6. THE Robot SHALL click the "Save" button in the AI_Config_Dialog to save the configuration
7. WHEN the save confirmation dialog appears, THE Robot SHALL click "OK" to dismiss the confirmation
8. WHEN the configuration is saved, THE Robot SHALL close the AI_Config_Dialog
9. IF the AI_Config_Dialog does not open within 5 seconds, THEN THE Test_Runner SHALL report the test case as FAIL

### Requirement 22: AI SQL Assistant

**User Story:** As a tester, I want the robot to use the AI SQL assistant to generate SQL from natural language, so that I can verify the AI assistant feature works end-to-end.

#### Acceptance Criteria

1. IF Requirement 21 (configure-ai) was skipped, THEN THE Test_Runner SHALL skip the ai-assistant test case with status SKIP
2. WHEN the ai-assistant test case executes, THE Robot SHALL click the AI Assistant toolbar button (🤖)
3. WHEN the AI_Assistant_Dialog is visible, THE Test_Runner SHALL verify that the dialog is open and ready for input
4. THE Robot SHALL type a natural language query into the input area, using the `ai_prompt` field from Test_Config if present, or defaulting to "Show all tables in the database"
5. THE Robot SHALL click the "Generate SQL" button in the AI_Assistant_Dialog
6. THE Robot SHALL wait up to 30 seconds for generated SQL to appear in the output text area
7. THE Test_Runner SHALL verify that the generated SQL text area is not empty
8. WHEN verification is complete, THE Robot SHALL close the AI_Assistant_Dialog
9. IF the AI_Assistant_Dialog does not open within 5 seconds, THEN THE Test_Runner SHALL report the test case as FAIL

### Requirement 10: Disconnect from Database

**User Story:** As a tester, I want the robot to disconnect from the database, so that I can verify the disconnect flow works correctly.

#### Acceptance Criteria

1. WHEN the disconnect test case executes, THE Robot SHALL click the "Disconnect DB" toolbar button
2. WHEN a confirmation dialog appears, THE Robot SHALL click "Yes" to confirm disconnection
3. THE Test_Runner SHALL verify that the status bar no longer shows an active connection for the test database
4. IF no confirmation dialog appears, THE Test_Runner SHALL still verify the disconnection state

### Requirement 11: Open and Verify About Dialog

**User Story:** As a tester, I want the robot to open the About dialog and verify its contents, so that I can confirm version information and buttons are present.

#### Acceptance Criteria

1. WHEN the about-dialog test case executes, THE Robot SHALL click the "About" toolbar button to open the About_Dialog
2. WHEN the About_Dialog is visible, THE Test_Runner SHALL verify that a version label containing "Version" text is present
3. THE Test_Runner SHALL verify that an "OK" button is present in the About_Dialog
4. THE Test_Runner SHALL verify that a "Check for Updates…" button is present in the About_Dialog
5. WHEN verification is complete, THE Robot SHALL click the "OK" button to close the About_Dialog
6. IF the About_Dialog does not open within 5 seconds, THEN THE Test_Runner SHALL report the test case as FAIL

### Requirement 12: Open and Verify Help Dialog

**User Story:** As a tester, I want the robot to open the Help dialog and verify all tabs are present, so that I can confirm the help content is accessible.

#### Acceptance Criteria

1. WHEN the help-dialog test case executes, THE Robot SHALL open the Help menu from the menu bar and click "User Guide"
2. WHEN the Help_Dialog is visible, THE Test_Runner SHALL verify that the following tabs exist: "User Guide", "DB Connection", "AI Configuration", "DB Health", "Execution Plan", "Themes"
3. THE Robot SHALL click each tab in sequence to verify each tab is selectable
4. WHEN all tabs are verified, THE Robot SHALL click the "Close" button to dismiss the Help_Dialog
5. IF the Help_Dialog does not open within 5 seconds, THEN THE Test_Runner SHALL report the test case as FAIL

### Requirement 13: Switch Themes

**User Story:** As a tester, I want the robot to switch the UI theme, so that I can verify theme switching works without errors.

#### Acceptance Criteria

1. WHEN the switch-theme test case executes, THE Robot SHALL locate the Theme_Combo in the toolbar
2. THE Robot SHALL select a different theme from the Theme_Combo (e.g., switch from "Flat Dark" to "Flat Light")
3. THE Test_Runner SHALL verify that the look and feel changes by checking that `UIManager.getLookAndFeel()` class name differs from the initial value
4. WHEN verification is complete, THE Robot SHALL restore the original theme by selecting the initial theme name in the Theme_Combo
5. IF the theme change does not take effect within 5 seconds, THEN THE Test_Runner SHALL report the test case as FAIL

### Requirement 14: Check for Updates from Help Menu

**User Story:** As a tester, I want the robot to trigger the update check from the Help menu, so that I can verify the update dialog opens correctly.

#### Acceptance Criteria

1. WHEN the check-updates test case executes, THE Robot SHALL open the Help menu from the menu bar
2. THE Robot SHALL click the "Check for Updates…" menu item
3. WHEN the UpdateDialog appears, THE Test_Runner SHALL verify that the dialog is visible and has a title containing "Updates"
4. THE Robot SHALL click the "Close" button to dismiss the UpdateDialog
5. IF the UpdateDialog does not appear within 5 seconds, THEN THE Test_Runner SHALL report the test case as FAIL

### Requirement 15: Clear Console

**User Story:** As a tester, I want the robot to clear the console panel, so that I can verify the clear functionality works.

#### Acceptance Criteria

1. WHEN the clear-console test case executes, THE Robot SHALL click the "Clear Console" toolbar button
2. THE Test_Runner SHALL verify that the Console_Panel text area is empty after the clear action
3. IF the Console_Panel still contains text after 2 seconds, THEN THE Test_Runner SHALL report the test case as FAIL

### Requirement 16: Delete Test Connection

**User Story:** As a tester, I want the robot to delete the test connection at the end of the test run, so that the application is left in a clean state.

#### Acceptance Criteria

1. WHEN the delete-connection test case executes, THE Robot SHALL right-click the test connection node in the Schema_Tree
2. WHEN the context menu appears, THE Robot SHALL click the "Delete" menu item
3. WHEN a confirmation dialog appears, THE Robot SHALL click "Yes" to confirm deletion
4. THE Test_Runner SHALL verify that the connection no longer appears in the Schema_Tree
5. IF the connection still appears after deletion, THEN THE Test_Runner SHALL report the test case as FAIL

### Requirement 17: Test Reporting and Logging

**User Story:** As a tester, I want the robot to produce a clear test report with PASS/FAIL status for each test case, so that I can quickly identify failures.

#### Acceptance Criteria

1. THE Test_Runner SHALL log each Test_Case name, status (PASS, FAIL, or SKIP), and execution duration in milliseconds to standard output
2. WHEN a Test_Case fails, THE Test_Runner SHALL log the failure reason including any exception message
3. WHEN all test cases complete, THE Test_Runner SHALL print a summary showing total tests run, total passed, and total failed
4. WHEN all test cases complete, THE Test_Runner SHALL exit with code 0 if all tests passed, or exit with code 1 if any test failed
5. THE Test_Runner SHALL execute test cases in the defined sequence: launch, add-connection, test-connection, connect, browse-schema, open-query-tab, run-query, verify-results, create-sample-data, export-ddl, view-schema-diagram, configure-ai, ai-assistant, disconnect, about-dialog, help-dialog, switch-theme, check-updates, clear-console, delete-connection

### Requirement 18: Robot Timing and Reliability

**User Story:** As a tester, I want the robot to handle UI timing reliably, so that tests do not fail due to race conditions or slow rendering.

#### Acceptance Criteria

1. THE Robot SHALL insert a configurable delay (default 500ms) between UI actions to allow Swing to process events
2. WHEN waiting for a dialog or component to appear, THE Robot SHALL poll at 100ms intervals up to the specified timeout
3. THE Robot SHALL use `SwingUtilities.invokeAndWait` for all component lookups and state verification to ensure thread safety
4. IF a test case encounters an unexpected exception, THEN THE Test_Runner SHALL catch the exception, report the test case as FAIL, and continue with the next test case
5. WHEN the test run completes (whether all tests pass or some fail), THE Test_Runner SHALL dispose the MainFrame and clean up resources
