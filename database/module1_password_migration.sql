-- Run once against the schema that owns the LoanFlow tables.
-- This is safe for existing rows: it only widens the password column.
DECLARE
    v_length NUMBER;
BEGIN
    SELECT data_length
      INTO v_length
      FROM user_tab_columns
     WHERE table_name = 'USERS'
       AND column_name = 'PASSWORD';

    IF v_length < 255 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE USERS MODIFY (PASSWORD VARCHAR2(255) NOT NULL)';
    END IF;
END;
/
