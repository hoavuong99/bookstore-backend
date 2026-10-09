package com.example.bookstore.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CatalogSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    void migrateCatalogMetadata() {
        migrateUnicodeCatalogColumns();
        addColumnIfMissing("books", "author_name", "NVARCHAR(255) NULL");
        addColumnIfMissing("books", "editors_pick", "BIT NOT NULL DEFAULT 0");
        addColumnIfMissing("categories", "image_url", "NVARCHAR(2048) NULL");
        dropColumnIfPresent("books", "rating");
        migratePaymentMethodConstraint();
    }

    private void migrateUnicodeCatalogColumns() {
        alterToUnicodeIfNeeded("books", "title", "NVARCHAR(255) NOT NULL");
        alterToUnicodeIfNeeded("books", "author_name", "NVARCHAR(255) NULL");
        alterUniqueColumnToUnicode("books", "isbn", "NVARCHAR(255) NOT NULL", "UX_books_isbn");
        alterToUnicodeIfNeeded("books", "description", "NVARCHAR(MAX) NULL");
        alterUniqueColumnToUnicode("categories", "name", "NVARCHAR(255) NOT NULL", "UX_categories_name");
        alterToUnicodeIfNeeded("categories", "description", "NVARCHAR(MAX) NULL");
    }

    private void alterUniqueColumnToUnicode(
            String tableName,
            String columnName,
            String definition,
            String replacementConstraintName
    ) {
        jdbcTemplate.execute("""
                IF EXISTS (
                    SELECT 1
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = 'dbo'
                      AND TABLE_NAME = '%s'
                      AND COLUMN_NAME = '%s'
                      AND DATA_TYPE IN ('char', 'varchar', 'text')
                )
                BEGIN
                    DECLARE @constraintName sysname;
                    SELECT TOP 1 @constraintName = kc.name
                    FROM sys.key_constraints kc
                    INNER JOIN sys.index_columns ic
                        ON kc.parent_object_id = ic.object_id
                       AND kc.unique_index_id = ic.index_id
                    INNER JOIN sys.columns c
                        ON ic.object_id = c.object_id
                       AND ic.column_id = c.column_id
                    WHERE kc.parent_object_id = OBJECT_ID('dbo.%s')
                      AND c.name = '%s';

                    IF @constraintName IS NOT NULL
                    BEGIN
                        EXEC('ALTER TABLE dbo.%s DROP CONSTRAINT [' + @constraintName + ']');
                    END;

                    ALTER TABLE dbo.%s ALTER COLUMN %s %s;

                    IF NOT EXISTS (
                        SELECT 1
                        FROM sys.key_constraints
                        WHERE parent_object_id = OBJECT_ID('dbo.%s')
                          AND name = '%s'
                    )
                    BEGIN
                        ALTER TABLE dbo.%s
                        ADD CONSTRAINT %s UNIQUE (%s);
                    END;
                END;
                """.formatted(
                tableName,
                columnName,
                tableName,
                columnName,
                tableName,
                tableName,
                columnName,
                definition,
                tableName,
                replacementConstraintName,
                tableName,
                replacementConstraintName,
                columnName
        ));
    }

    private void alterToUnicodeIfNeeded(String tableName, String columnName, String definition) {
        jdbcTemplate.execute("""
                IF EXISTS (
                    SELECT 1
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = 'dbo'
                      AND TABLE_NAME = '%s'
                      AND COLUMN_NAME = '%s'
                      AND DATA_TYPE IN ('char', 'varchar', 'text')
                )
                BEGIN
                    ALTER TABLE dbo.%s ALTER COLUMN %s %s;
                END;
                """.formatted(tableName, columnName, tableName, columnName, definition));
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        Integer columnLength = jdbcTemplate.queryForObject(
                "SELECT COL_LENGTH('dbo." + tableName + "', ?)",
                Integer.class,
                columnName
        );
        if (columnLength == null) {
            jdbcTemplate.execute("ALTER TABLE dbo." + tableName + " ADD " + columnName + " " + definition);
        }
    }

    private void dropColumnIfPresent(String tableName, String columnName) {
        jdbcTemplate.execute("""
                IF COL_LENGTH('dbo.%s', '%s') IS NOT NULL
                BEGIN
                    ALTER TABLE dbo.%s DROP COLUMN %s;
                END;
                """.formatted(tableName, columnName, tableName, columnName));
    }

    private void migratePaymentMethodConstraint() {
        jdbcTemplate.execute("""
                IF EXISTS (
                    SELECT 1
                    FROM sys.check_constraints
                    WHERE parent_object_id = OBJECT_ID('dbo.orders')
                      AND definition LIKE '%payment_method%'
                      AND definition NOT LIKE '%ZALOPAY%'
                )
                BEGIN
                    DECLARE @constraintName sysname;
                    SELECT TOP 1 @constraintName = name
                    FROM sys.check_constraints
                    WHERE parent_object_id = OBJECT_ID('dbo.orders')
                      AND definition LIKE '%payment_method%'
                      AND definition NOT LIKE '%ZALOPAY%';
                    EXEC('ALTER TABLE dbo.orders DROP CONSTRAINT [' + @constraintName + ']');
                END;

                IF NOT EXISTS (
                    SELECT 1
                    FROM sys.check_constraints
                    WHERE parent_object_id = OBJECT_ID('dbo.orders')
                      AND name = 'CK_orders_payment_method'
                )
                BEGIN
                    ALTER TABLE dbo.orders
                    ADD CONSTRAINT CK_orders_payment_method
                    CHECK (payment_method IN ('COD', 'ZALOPAY', 'VNPAY', 'MOMO'));
                END;
                """);
    }
}
