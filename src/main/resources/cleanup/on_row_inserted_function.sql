CREATE FUNCTION test_on_row_inserted() RETURNS TRIGGER AS
$body$
BEGIN
    INSERT INTO t_tests_changes_tracking VALUES (TG_TABLE_NAME::varchar);
    RETURN NULL;
END;
$body$
    LANGUAGE plpgsql;
