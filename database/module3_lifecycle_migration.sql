-- Safe, additive lifecycle migration for the existing LoanFlow schema.
DECLARE
    v_count NUMBER;
    PROCEDURE add_column_if_missing(p_table VARCHAR2, p_column VARCHAR2, p_sql VARCHAR2) IS
        v_column_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_column_count FROM user_tab_columns
        WHERE table_name = p_table AND column_name = p_column;
        IF v_column_count = 0 THEN EXECUTE IMMEDIATE p_sql; END IF;
    END;
BEGIN
    add_column_if_missing('PAYMENT', 'PAYMENT_METHOD',
        'ALTER TABLE PAYMENT ADD (PAYMENT_METHOD VARCHAR2(30))');
    add_column_if_missing('PAYMENT', 'PAYMENT_REFERENCE',
        'ALTER TABLE PAYMENT ADD (PAYMENT_REFERENCE VARCHAR2(100))');
    add_column_if_missing('LOAN', 'CLOSED_DATE',
        'ALTER TABLE LOAN ADD (CLOSED_DATE DATE)');

    SELECT COUNT(*) INTO v_count FROM user_indexes
    WHERE index_name = 'UX_LOAN_APPLICATION';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX UX_LOAN_APPLICATION ON LOAN(APPLICATION_ID)';
    END IF;
END;
/

-- Reconcile only legacy approved applications that have complete loan terms.
-- Applications with incomplete historical data are intentionally left untouched.
INSERT INTO LOAN (
    APPLICATION_ID, CUSTOMER_ID, LOAN_AMOUNT, INTEREST_RATE,
    TENURE_MONTHS, EMI_AMOUNT, START_DATE, STATUS
)
SELECT
    la.APPLICATION_ID, la.CUSTOMER_ID, la.LOAN_AMOUNT, la.INTEREST_RATE,
    la.TENURE_MONTHS, la.EMI_AMOUNT, NVL(la.APPLICATION_DATE, SYSDATE), 'ACTIVE'
FROM LOAN_APPLICATION la
WHERE la.STATUS = 'APPROVED'
  AND la.INTEREST_RATE IS NOT NULL
  AND la.TENURE_MONTHS IS NOT NULL
  AND la.EMI_AMOUNT IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM LOAN l WHERE l.APPLICATION_ID = la.APPLICATION_ID
  );

COMMIT;
