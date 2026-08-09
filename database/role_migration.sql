-- Loan Management System - Role Based Login Migration
-- Run this once in Oracle SQL*Plus using the same schema that owns USERS.

ALTER TABLE USERS ADD (
    ROLE VARCHAR2(20) DEFAULT 'CUSTOMER' NOT NULL
);

ALTER TABLE USERS ADD CONSTRAINT CK_USERS_ROLE
CHECK (ROLE IN ('CUSTOMER', 'LOAN_OFFICER', 'ADMIN'));

-- Existing users become customers by default.
UPDATE USERS SET ROLE = 'CUSTOMER' WHERE ROLE IS NULL;
COMMIT;

-- Create staff accounts only after changing the sample values below.
-- Never share real passwords in source code or Git.
--
-- INSERT INTO USERS (FULL_NAME, EMAIL, PASSWORD, ROLE)
-- VALUES ('System Administrator', 'admin@example.com', 'CHANGE_ME', 'ADMIN');
--
-- INSERT INTO USERS (FULL_NAME, EMAIL, PASSWORD, ROLE)
-- VALUES ('Loan Officer', 'officer@example.com', 'CHANGE_ME', 'LOAN_OFFICER');
--
-- COMMIT;
