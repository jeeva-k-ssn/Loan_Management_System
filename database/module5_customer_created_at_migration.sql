-- Additive migration for accurate new-customer reporting.
-- Existing rows remain NULL because their original registration date is unknown.
DECLARE
    v_column_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_column_count
      FROM user_tab_columns
     WHERE table_name = 'USERS'
       AND column_name = 'CREATED_AT';

    IF v_column_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE USERS ADD (CREATED_AT DATE)';
    END IF;
END;
/
COMMIT;
