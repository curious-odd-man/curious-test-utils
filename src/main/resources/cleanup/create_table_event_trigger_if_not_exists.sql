-- Create function and EVENT TRIGGER if not already created
-- Unfortunately there is no IF NOT EXISTS clause for CREATE EVENT TRIGGER and for CREATE FUNCTION, so I had to
-- create this code to overcome this.
-- NOTE: Event triggers are created for whole database (for all schemas at once).
DO
$body$
    DECLARE
        count INTEGER;
    BEGIN
        SELECT count(*)
        INTO count
        FROM pg_event_trigger
        WHERE evtname = 'create_table_trigger';

        IF count = 0 THEN
            -- A function to be triggered when CREATE TABLE is executed
            EXECUTE 'CREATE FUNCTION on_create_table_event() RETURNS EVENT_TRIGGER AS
                $innerbody$
                DECLARE
                    obj record;
                BEGIN
                    FOR obj IN SELECT * FROM pg_event_trigger_ddl_commands()
                        LOOP
                            IF obj.object_type = ''table''::text THEN
                                PERFORM test_track_inserts(obj.objid);
                            END IF;
                        END LOOP;
                END ;
                $innerbody$
                    language ''plpgsql'';';

            -- Create event trigger to call function above when CREATE TABLE is executed
            EXECUTE 'CREATE EVENT TRIGGER create_table_trigger ON ddl_command_end WHEN TAG in (''CREATE TABLE'') EXECUTE FUNCTION on_create_table_event();';
        ELSE
            RAISE NOTICE 'create_table_trigger event trigger already exists';
        END IF;
    END;
$body$
language 'plpgsql';
