-- Function to register a table for updates
-- Whenever INSERT INTO is executed procedure test_on_row_inserted should be called
CREATE FUNCTION test_track_inserts(target_table regclass) RETURNS void AS
$body$
DECLARE
    _q_txt        text;
    _trigger_name text;
BEGIN
    IF target_table = 't_tests_changes_tracking'::regclass THEN
        RETURN;
    END IF;
    _trigger_name = 'tg_insert_' || target_table;
    IF length(_trigger_name) > 63 THEN
        _trigger_name = LEFT(_trigger_name, 63);
    END IF;
    EXECUTE 'DROP TRIGGER IF EXISTS ' || _trigger_name || ' ON ' || target_table;

    _q_txt = 'CREATE TRIGGER ' || _trigger_name || ' BEFORE INSERT ON ' ||
             target_table ||
             ' FOR EACH STATEMENT EXECUTE PROCEDURE test_on_row_inserted();';
    EXECUTE _q_txt;
END
$body$
    language 'plpgsql';
