-- Safe, additive role migration for the current LoanFlow USERS table.
-- Run this once only if USER_ROLE is not already present.
DECLARE
    v_column_count NUMBER;
    v_constraint_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_column_count
      FROM user_tab_columns
     WHERE table_name = 'USERS' AND column_name = 'USER_ROLE';

    IF v_column_count = 0 THEN
        EXECUTE IMMEDIATE
            'ALTER TABLE USERS ADD (USER_ROLE VARCHAR2(20) DEFAULT ''CUSTOMER'' NOT NULL)';
    END IF;

    SELECT COUNT(*) INTO v_constraint_count
      FROM user_constraints
     WHERE table_name = 'USERS' AND constraint_name = 'CK_USERS_USER_ROLE';

    IF v_constraint_count = 0 THEN
        EXECUTE IMMEDIATE
            'ALTER TABLE USERS ADD CONSTRAINT CK_USERS_USER_ROLE '
            || 'CHECK (USER_ROLE IN (''CUSTOMER'', ''LOAN_OFFICER'', ''ADMIN''))';
    END IF;
END;
/

UPDATE USERS SET USER_ROLE = 'CUSTOMER' WHERE USER_ROLE IS NULL;
COMMIT;
