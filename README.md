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

The current database connection is:

```text
jdbc:oracle:thin:@localhost:1521:XE
username: system
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
```

The later migrations are additive. They enable password upgrades, roles, loan lifecycle fields, notifications, and customer creation dates.

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

