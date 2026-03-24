package com.dbexplorer.service;

import com.dbexplorer.model.DatabaseType;
import com.dbexplorer.model.LazyQueryResult;
import com.dbexplorer.model.QueryResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class QueryExecutor {

    private final ExecutorService executor = new ThreadPoolExecutor(
            2, 8, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(50),
            r -> {
                Thread t = new Thread(r, "query-exec");
                t.setDaemon(true);
                return t;
            }
    );

    /**
     * Execute SQL asynchronously. For SELECT queries, returns a LazyQueryResult
     * via onLazyResult. For non-SELECT, returns a QueryResult via onSuccess.
     */
    public Future<?> executeAsync(Connection connection, String sql,
                                  Consumer<LazyQueryResult> onLazyResult,
                                  Consumer<QueryResult> onSuccess,
                                  Consumer<SQLException> onError) {
        return executor.submit(() -> {
            try {
                long start = System.currentTimeMillis();
                Statement stmt = connection.createStatement();
                stmt.setFetchSize(LazyQueryResult.DEFAULT_FETCH_SIZE);
                boolean hasResultSet = stmt.execute(sql);
                long elapsed = System.currentTimeMillis() - start;

                if (hasResultSet) {
                    ResultSet rs = stmt.getResultSet();
                    LazyQueryResult lazy = new LazyQueryResult(stmt, rs, elapsed);
                    onLazyResult.accept(lazy);
                } else {
                    int affected = stmt.getUpdateCount();
                    stmt.close();
                    onSuccess.accept(new QueryResult(List.of(), List.of(), elapsed, affected, false));
                }
            } catch (SQLException e) {
                onError.accept(e);
            }
        });
    }

    /**
     * Fetch the next page from a LazyQueryResult asynchronously.
     */
    public Future<?> fetchNextPageAsync(LazyQueryResult lazyResult,
                                        Consumer<List<List<Object>>> onPage,
                                        Consumer<SQLException> onError) {
        return executor.submit(() -> {
            try {
                List<List<Object>> page = lazyResult.fetchNextPage();
                onPage.accept(page);
            } catch (SQLException e) {
                onError.accept(e);
            }
        });
    }

    /**
     * Execute EXPLAIN/execution plan asynchronously.
     * Generates the appropriate EXPLAIN syntax per database type.
     */
    public Future<?> explainAsync(Connection connection, String sql, DatabaseType dbType,
                                  Consumer<String> onSuccess, Consumer<SQLException> onError) {
        return executor.submit(() -> {
            try {
                String plan = executeExplain(connection, sql, dbType);
                onSuccess.accept(plan);
            } catch (SQLException e) {
                onError.accept(e);
            }
        });
    }

    private String executeExplain(Connection connection, String sql, DatabaseType dbType)
            throws SQLException {
        StringBuilder plan = new StringBuilder();

        switch (dbType) {
            case POSTGRESQL -> {
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("EXPLAIN ANALYZE " + sql)) {
                    while (rs.next()) {
                        plan.append(rs.getString(1)).append("\n");
                    }
                }
            }
            case MYSQL -> {
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("EXPLAIN " + sql)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    // Pipe-delimited format that ExplainPlanPanel can parse as a table
                    StringBuilder header = new StringBuilder("|");
                    StringBuilder sep    = new StringBuilder("+");
                    for (int i = 1; i <= cols; i++) {
                        String label = meta.getColumnLabel(i);
                        header.append(" ").append(label).append(" |");
                        sep.append("-".repeat(label.length() + 2)).append("+");
                    }
                    plan.append(sep).append("\n")
                        .append(header).append("\n")
                        .append(sep).append("\n");
                    while (rs.next()) {
                        plan.append("|");
                        for (int i = 1; i <= cols; i++) {
                            String val = rs.getString(i);
                            plan.append(" ").append(val == null ? "NULL" : val).append(" |");
                        }
                        plan.append("\n");
                    }
                    plan.append(sep).append("\n");
                }
            }
            case ORACLE -> {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("EXPLAIN PLAN FOR " + sql);
                    try (ResultSet rs = stmt.executeQuery(
                            "SELECT PLAN_TABLE_OUTPUT FROM TABLE(DBMS_XPLAN.DISPLAY())")) {
                        while (rs.next()) {
                            plan.append(rs.getString(1)).append("\n");
                        }
                    }
                }
            }
            case SQLSERVER -> {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("SET SHOWPLAN_TEXT ON");
                    try (ResultSet rs = stmt.executeQuery(sql)) {
                        while (rs.next()) {
                            plan.append(rs.getString(1)).append("\n");
                        }
                    }
                    stmt.execute("SET SHOWPLAN_TEXT OFF");
                }
            }
            case DYNAMODB -> {
                plan.append("Explain Plan is not supported for DynamoDB.");
            }
        }

        return plan.toString();
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
