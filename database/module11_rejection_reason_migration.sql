-- Additive rejection-reason support for LoanFlow applications.
-- Run this once after the existing application migrations.
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM user_tab_columns
    WHERE table_name = 'LOAN_APPLICATION' AND column_name = 'REJECTION_REASON';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE LOAN_APPLICATION ADD (REJECTION_REASON VARCHAR2(120))';
    END IF;
END;
/

COMMIT;
