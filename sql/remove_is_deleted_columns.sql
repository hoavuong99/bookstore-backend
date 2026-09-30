SET NOCOUNT ON;
GO

DECLARE @tables TABLE (table_name NVARCHAR(128));
INSERT INTO @tables (table_name)
VALUES
    ('users'),
    ('books'),
    ('carts'),
    ('cart_items'),
    ('coupons'),
    ('orders'),
    ('order_items');

DECLARE @tableName NVARCHAR(128);
DECLARE @schemaName NVARCHAR(128) = 'dbo';
DECLARE @constraintName NVARCHAR(128);
DECLARE @sql NVARCHAR(MAX);

DECLARE table_cursor CURSOR FOR
SELECT table_name FROM @tables;

OPEN table_cursor;
FETCH NEXT FROM table_cursor INTO @tableName;

WHILE @@FETCH_STATUS = 0
BEGIN
    IF EXISTS (
        SELECT 1
        FROM sys.columns c
        JOIN sys.tables t ON c.object_id = t.object_id
        JOIN sys.schemas s ON t.schema_id = s.schema_id
        WHERE t.name = @tableName
          AND s.name = @schemaName
          AND c.name = 'is_deleted'
    )
    BEGIN
        SELECT TOP 1 @constraintName = dc.name
        FROM sys.default_constraints dc
        JOIN sys.columns c
            ON c.default_object_id = dc.object_id
        JOIN sys.tables t
            ON t.object_id = c.object_id
        JOIN sys.schemas s
            ON t.schema_id = s.schema_id
        WHERE t.name = @tableName
          AND s.name = @schemaName
          AND c.name = 'is_deleted';

        IF @constraintName IS NOT NULL
        BEGIN
            SET @sql = N'ALTER TABLE [' + @schemaName + N'].[' + @tableName + N'] DROP CONSTRAINT [' + @constraintName + N']';
            EXEC sp_executesql @sql;
            SET @constraintName = NULL;
        END

        SET @sql = N'ALTER TABLE [' + @schemaName + N'].[' + @tableName + N'] DROP COLUMN [is_deleted]';
        EXEC sp_executesql @sql;
    END

    FETCH NEXT FROM table_cursor INTO @tableName;
END

CLOSE table_cursor;
DEALLOCATE table_cursor;
GO
