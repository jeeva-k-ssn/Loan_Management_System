# LoanFlow — Loan Management System

LoanFlow is a JavaFX desktop application for managing lending operations from loan application through repayment and closure. It connects to Oracle Database 21c XE using JDBC and provides role-based workspaces for customers, loan officers, and administrators.

> **Tagline:** Smarter Lending. Simpler Management.

## Features

- Secure login with role-based access control
- Customer registration and profile management
- EMI and repayment calculations
- Loan application submission and status tracking
- Loan officer approval and rejection workflow
- Automatic loan creation after approval
- Payment recording and loan closure
- Loan statements, repayment history, and outstanding balances
- In-app notifications
- Credit decision support combines simulated CIBIL data, EMI-to-income burden, and LoanFlow repayment history. Applicants with no prior LoanFlow repayments are not automatically rejected; they remain `REQUIRES_REVIEW` for officer verification.
- Admin user, customer, application, loan, and payment monitoring
- Admin statistics and charts
- Maximized desktop presentation with responsive JavaFX layouts

## User roles and permissions 

### Customer

Customers can register, sign in, calculate EMI, submit loan applications, view application and loan status, record repayments, view statements, read notifications, update their profile, and log out.

### Loan Officer

Loan officers can review pending applications, inspect application details, approve or reject applications, create active loans from approved applications, monitor loan portfolios, and receive workflow notifications.

### Administrator

Administrators can manage users and roles, review customers, applications, loans, and payments, open customer profiles, view operational metrics, inspect charts, and refresh administrative data.

## Main workflows

### Customer loan workflow

```text
Login → Customer Dashboard → Calculate EMI → Submit Loan Application
     → Application Pending → Officer Review → Approved Loan
     → Record Payments → Loan Statement → Loan Closed
```

### Loan officer workflow

```text
Login → Officer Dashboard → Pending Applications 
     → Review Application → Approve or Reject
     → Notification and Dashboard Refresh
```

### Administrator workflow

```text
Login → Admin Dashboard → Review Users, Customers, Applications and Payments
     → Inspect Metrics and Charts → Open Customer Profile → Refresh or Log Out
```

## Credit information sources

LoanFlow intentionally supports two separate credit-information sources.

### Simulated external bureau

The mock TransUnion CIBIL integration represents an external credit-bureau connection. After customer consent is recorded, a loan officer retrieves the simulated report for the selected application. The report is stored as an immutable application snapshot and contributes to the academic credit assessment.

The simulated report includes:

- CIBIL score
- Payment history percentage
- Credit utilization
- Active accounts
- Recent enquiries
- Defaults
- Total outstanding amount

### Customer-uploaded supporting report

The customer-uploaded report is an additional supporting document, not a replacement for the simulated bureau integration. A customer may upload a PDF, and an administrator can review and mark it as verified or rejected. This represents manual document verification, while the simulated CIBIL report represents an external bureau workflow used during loan assessment.

In short:

```text
Uploaded report     -> Optional supporting document -> Admin verification
Simulated CIBIL     -> External bureau simulation   -> Officer assessment
```

LoanFlow does not connect to or claim live CIBIL data.

## Credit assessment workflow

The intended assessment sequence is:

```text
Customer submits application
        -> Credit consent is recorded
        -> Application enters PENDING status
        -> Officer opens the application
        -> Officer retrieves the simulated CIBIL report
        -> LoanFlow combines CIBIL, income, EMI burden, and repayment history
        -> Officer reviews the decision-support assessment
        -> Officer approves or rejects the application
```

The customer-entered monthly income is synchronized into `LOANFLOW_CREDIT_PROFILE` when the application is submitted. The assessment uses the latest profile income and the CIBIL snapshot retrieved for that application.

## Assessment policy

The assessment is decision support only; the loan officer makes the final decision.

| Condition | Result | Meaning |
| --- | --- | --- |
| EMI-to-income ratio above 50%, or CIBIL below 600 | `NOT_ELIGIBLE` / `HIGH` | A configured LoanFlow limit has failed |
| Income or CIBIL score is unavailable | `REQUIRES_REVIEW` | Required information needs officer verification |
| No prior LoanFlow repayment history | `REQUIRES_REVIEW` | The applicant is not automatically rejected, but has no internal repayment evidence |
| EMI-to-income ratio at or below 35%, CIBIL at least 700, and repayment history available | `ELIGIBLE` / `LOW` | The configured score and affordability checks are within the strong range |
| Other combinations within broad limits | `REQUIRES_REVIEW` / `MEDIUM` | The complete profile needs officer judgment |

The absence of prior LoanFlow repayments is therefore a review condition, not an automatic rejection. A new applicant may have a strong simulated CIBIL score and affordable EMI while still requiring manual review because LoanFlow has no repayment history for that customer.

## Consent and application integrity

- EMI calculation requires the customer to authorize credit retrieval.
- Submission checks the consent checkbox again, so consent cannot become stale after EMI calculation.
- `CREDIT_CONSENT_GIVEN` is written from the current submission state.
- The generated application ID from the database insert is used directly for notifications and downstream processing.
- Rejection reasons are stored with rejected applications and included in customer notifications.

## Rejection reasons

When rejecting an application, the officer selects one of the configured reasons:

- Insufficient income
- High existing debt
- Low credit score
- High EMI burden
- Incomplete information
- Other

The selected reason is stored in `LOAN_APPLICATION.REJECTION_REASON` and is visible in the customer application history.

## Data integrity and concurrency safeguards

- Approval and loan creation are handled as a transactional workflow.
- Payment processing locks the selected loan row before checking the outstanding balance, preventing concurrent payments from passing the same stale balance check.
- Payment validation prevents payments greater than the current outstanding balance.
- A fully repaid loan is marked `CLOSED` after the payment transaction completes.
- Changing a user to `CUSTOMER` creates an `LMS_CUSTOMER` profile when one does not already exist, preventing role/profile inconsistencies.
- Database-heavy operations run through JavaFX background tasks, while alerts, validation messages, navigation, and other UI changes are marshalled to the JavaFX Application Thread.

## Academic-project scope

LoanFlow is designed as an academic lending-system demonstration. The CIBIL source is deterministic mock data, uploaded reports are local supporting documents, and the assessment is not a regulated lending or credit-scoring decision. A production system would require stronger identity verification, secure document storage, audit controls, authorization hardening, database isolation review, monitoring, and formal compliance review.

## Technology stack

| Technology | Version / purpose |
| --- | --- |
| Java | 17, Temurin recommended |
| JavaFX | 17.0.11 controls and FXML |
| Maven | 3.9 or later |
| Oracle Database | 21c XE |
| Oracle JDBC | `ojdbc11` 23.8.0.25.04 |
| FXML | JavaFX view definitions |
| JavaFX CSS | Application styling |
| JUnit | 5.11 test suite |
| VS Code | Recommended IDE |
| Git | Source control |

## Requirements

Install the following on another computer:

1. JDK 17 and `JAVA_HOME`.
2. Apache Maven 3.9+ on `PATH`.
3. Oracle Database 21c XE running locally.
4. An Oracle schema with permission to use the LoanFlow tables.
5. Git, if cloning the repository.

Check installations:

```bash
java -version
javac -version
mvn -version
```

The default database connection is:

```text
jdbc:oracle:thin:@localhost:1521:XE
username: `LMS_DB_USER` if configured, otherwise `system`
```

The password is never stored in source code. Configure `LMS_DB_PASSWORD`.

## Database setup

Start Oracle XE, create/select the application schema, then apply the base schema and migrations in `database/`.

Recommended migration order:

```text
module1_password_migration.sql
role_migration.sql
module3_lifecycle_migration.sql
module4_notifications_migration.sql
module5_customer_created_at_migration.sql
module6_credit_assessment_migration.sql
module7_credit_verification_migration.sql
module8_credit_report_upload_migration.sql
module9_mock_credit_bureau_migration.sql
module10_quality_hardening_migration.sql
module11_rejection_reason_migration.sql
module12_emi_tracker_migration.sql
```

The migrations are additive and do not delete existing records. Modules 7 and 8 preserve the legacy verification/document-upload capability. Module 9 adds the simulated credit-bureau source, customer consent, applicant identity/employment fields, and application retrieval snapshots. Module 10 adds deterministic demonstration scenarios and officer/admin audit logging. LoanFlow does not retrieve or claim real CIBIL data.

Example connection:

```bash
sqlplus system@localhost:1521/XE
```

Never commit database passwords or production data.

## Configure the database password

### Windows PowerShell

Current terminal only:

```powershell
$env:LMS_DB_PASSWORD = "your-oracle-password"
$env:LMS_DB_USER = "LOANFLOW_APP" # optional; defaults to SYSTEM for local academic setup
```

Persist for the Windows user:

```powershell
[Environment]::SetEnvironmentVariable("LMS_DB_PASSWORD", "your-oracle-password", "User")
```

### Windows Command Prompt

```cmd
set LMS_DB_PASSWORD=your-oracle-password
```

### Linux or macOS

```bash
export LMS_DB_PASSWORD='your-oracle-password'
```

## Build, test, and run

```bash
git clone <repository-url>
cd loan-management-system
mvn clean compile
mvn test
mvn clean package
mvn javafx:run
```

Run `mvn javafx:run` from the same terminal where `LMS_DB_PASSWORD` is configured.

## Project structure

```text
loan-management-system/
├── database/                         Oracle migration scripts
├── src/
│   ├── main/
│   │   ├── java/com/loanmanagement/
│   │   │   ├── controller/           JavaFX controllers
│   │   │   ├── database/             JDBC utilities
│   │   │   ├── model/                Domain models
│   │   │   ├── navigation/           Stage/navigation management
│   │   │   ├── service/              Authentication and notification services
│   │   │   └── util/                 Password, EMI, and alert utilities
│   │   └── resources/
│   │       ├── css/                  JavaFX stylesheets
│   │       └── fxml/                 JavaFX views
│   └── test/java/                    Unit, FXML, audit, and database tests
├── pom.xml
└── README.md
```

## Architecture

- `Main` creates the primary maximized JavaFX stage.
- `NavigationManager` keeps navigation inside the application window.
- Controllers manage screen interactions.
- Services handle authentication and notifications.
- `DatabaseConnection` provides Oracle JDBC connections.
- JavaFX `Task` workers handle database-heavy loading and actions.
- Loan calculations are kept in utility classes and tested separately.

## Testing

The suite includes EMI calculation, FXML/controller loading, database audit, and available end-to-end database tests.

Run the complete suite:

```bash
mvn clean test
```

If database tests fail, verify Oracle XE, the `XE` service, migrations, and `LMS_DB_PASSWORD`.

## Common problems

### Database password error

Set `LMS_DB_PASSWORD` and restart the terminal or IDE.

### Connection refused on port 1521

Start Oracle XE and confirm the listener/service name is `XE`.

### JavaFX runtime errors

Use JDK 17 and run through Maven:

```bash
mvn javafx:run
```

### FXML loading errors

```bash
mvn test -Dtest=LoanManagementFxmlTest
```

Check controller names, `fx:id` values, and `onAction` methods.

## Security notes

- Never commit `LMS_DB_PASSWORD` or other secrets.
- Use a restricted Oracle application account instead of `SYSTEM` for production.
- Password handling is managed through the application password utility.
- This project is intended for educational/development use and should receive a security review before production.

## Author

Jeeva K  
SSN College of Engineering
