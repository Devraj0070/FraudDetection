# 💳 SafePay — AI-Powered Fraud Detection System

<p align="center">
  <img src="https://img.shields.io/badge/Java-25-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 25" />
  <img src="https://img.shields.io/badge/Spring%20Boot-Backend-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Java%20Swing-Desktop%20UI-4479A1?style=for-the-badge&logo=java&logoColor=white" alt="Java Swing" />
  <img src="https://img.shields.io/badge/MySQL-Database-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL" />
  <img src="https://img.shields.io/badge/Weka-Machine%20Learning-8A2BE2?style=for-the-badge" alt="Weka" />
</p>

<p align="center">
  <b>🛡️ Smarter Transactions. Safer Payments. Intelligent Fraud Detection.</b>
</p>

<<<<<<< HEAD
> An end-to-end academic fraud detection platform that combines Machine Learning, rule-based detection, behavioral analysis, transaction validation, secure authentication, database persistence, a complete standalone Java Swing desktop application.
=======
SafePay is an **AI-powered fraud detection system** built with Java. It combines transaction analysis, fraud-risk assessment, machine-learning components, and rule-based detection to identify potentially suspicious financial activity.

The project features a **Java Swing and Java2D desktop application** connected to a **Spring Boot REST API**, with MySQL for data storage and Weka for machine-learning functionality. It also includes transaction validation, behavioral analysis, secure authentication, and database persistence.

=======
SafePay is an **AI-powered fraud detection system** built with Java. It combines transaction analysis, fraud-risk assessment, machine-learning components, and a desktop interface to help identify potentially suspicious financial activity.

The project uses a **Java Swing and Java2D desktop application** connected to a **Spring Boot REST API**, with MySQL for data storage and Weka for machine-learning functionality.
>>>>>>> 3b7cfc8 (Update SafePay application and add frontend)

---

## ✨ Key Features

### ðŸ–¥ï¸ Modern Desktop Application
- ðŸ” Login and registration interface.
- 📊 Dashboard for transaction and account summaries.
- 💸 Transaction submission interface.
- 📜 Transaction history view.
- 📈 Java2D transaction-amount chart.
- 🛡️ï¸ Admin monitoring interface for fraud alerts.

### 🔌 Backend and REST API
- âš™ï¸ Spring Boot REST API.
- 🔑 Spring Security authentication and authorization.
- 🛡️ï¸ CSRF protection for API requests.
- 👤 Transaction ownership checks.
- 📡 API endpoints for authentication, dashboard data, transactions, and administrative operations.
- âš ï¸ Centralized exception handling.

### 🤖 Fraud Detection and Machine Learning
- 🔎 Rule-based fraud checks.
- 📉 Transaction feature extraction.
- 🧠 Weka-based machine-learning components.
- 🧪 Dataset loading and preprocessing utilities.
- 📊 Model-training and evaluation utilities where available.

### 🧪 Testing and Reliability
- ✅ Automated tests for backend services and REST controllers.
- ðŸ” Authentication and authorization tests.
- 💳 Transaction-processing tests.
- 🧠 Fraud-rule and feature-extraction tests.
- ðŸ–¥ï¸ Desktop UI tests.

---

## ðŸ› ï¸ Technology Stack

The project was developed to demonstrate a complete fraud-detection workflow using Java and related technologies.

## Main Objectives

- Build a working banking-style transaction system.
- Detect potentially fraudulent transactions.
- Integrate a trained Machine Learning model into a Java application.
- Implement explainable rule-based fraud detection.
- Detect suspicious transaction behavior.
- Prevent blocked transactions from changing account balances.
- Distinguish fraud from normal business-rule failures.
- Implement secure authentication and role-based authorization.
- Store application data in MySQL.
- Provide transaction results and history through a Swing desktop interface.
- Provide administrative fraud alerts.
- Include a desktop monitoring/simulation application.
- Include automated tests for major components.
- Maintain a modular and understandable project structure.
- HTTP controllers
- Services
- Dependency Injection
- Database integration
- Security
- Configuration
- Testing
- Web application startup
- Authentication
- Login
- Role-based authorization
- Password verification
- CSRF protection
- Security headers
- Protected application routes

---

| Technology | Purpose |
|---|---|
| â˜• **Java 25** | Core programming language |
| ðŸƒ **Spring Boot** | Backend application and REST API |
| ðŸ–¥ï¸ **Java Swing** | Native desktop interface |
| ðŸŽ¨ **Java2D** | Custom charts and desktop graphics |
| ðŸ” **Spring Security** | Authentication and authorization |
| 🧠 **Weka** | Machine-learning functionality |
| ðŸ—„ï¸ **MySQL 8** | Relational database |
| 📦 **Maven Wrapper** | Build and dependency management |
| 🧪 **JUnit / Maven Surefire** | Automated testing, where configured |
| ðŸŒ¿ **Git and GitHub** | Version control and collaboration |

> Check `pom.xml` for the exact dependency versions and test libraries configured in the current source code.

---

## ðŸ—ï¸ System Architecture

```text
       👤 User / Administrator
                  |
                  v
      ðŸ–¥ï¸ Java Swing Desktop App
              + Java2D
                  |
                  v
          🔌 REST API Client
                  |
                  v
       ðŸƒ Spring Boot REST API
                  |
          +-------+-------+
          |       |       |
          v       v       v
       ðŸ” Auth  💳 Fraud  📊 Dashboard
          |       |
          v       v
       ðŸ—„ï¸ MySQL  🧠 Weka / Risk Rules
```

The desktop client communicates with the backend through HTTP API requests. The backend handles application logic, security checks, and data access.

---

The project was developed to demonstrate a complete fraud-detection workflow using Java and related technologies.

## Main Objectives

- `User`
- `Account`
- `Transaction`
- `FraudPrediction`
- `FraudAlert`
- Account balance
- Transaction status
- Fraud probability
- Risk level
- Detection reason
- Transaction history
- Fraud detection strategies
- Transaction processing
- Fraud alerts
- Monitoring
- Transaction simulation
- 📦 Download and manage dependencies declared in `pom.xml`.
- ðŸ—ï¸ Compile the Java source code.
- 🧪 Run automated tests.
- 📦 Package the application into a build artifact.
- ðŸ” Run repeatable build tasks using standard commands.
- Multiple transactions in a short time.
- Repeated large transactions.
- Large transactions combined with recent activity.
- Very large transactions combined with previous activity.
- High transaction frequency.
- Learning patterns from historical data.
- Producing a probability-based prediction.
- Detecting statistical relationships.
- Explicit security requirements.
- Business constraints.
- Explainable behavioral patterns.
- Deterministic decisions.
- The input is valid.
- Fraud analysis does not block it.
- The amount is within the configured transaction limit.
- The account has enough balance.
- Does not modify the account balance.
- Can generate a fraud alert.
- Is displayed as high risk.
- Shows the relevant detection reason.
- Register.
- Log in.
- View their dashboard.
- Submit transactions.
- View transaction results.
- View transaction history.
- Validation
- Account lookup
- ML prediction
- Rule analysis
- Limit checks
- Balance checks
- Balance updates
- Transaction status
- Fraud-related information
- Rule detection
- Risk calculation
- Feature extraction
- ML prediction
- Fraud monitoring
- Transaction simulation
- Fraud strategy demonstration
- Fraud alerts
- Transaction processing
- Data-table presentation
- ML prediction
- ML fraud probability
- Final security decision
- Detection reason
- Risk assessment
- Transaction status
- Admin controller
- Dashboard controller
- Transaction controller
- Random Forest predictor
- Fraud detection strategies
- GUI table models
- Transaction simulation
- JDBC database connection
- Transaction records
- Dataset analysis
- Dataset loading
- Model evaluation
- Model saving
- Model training
- Admin authorization
- User details service
- Transaction service
- Metrics servlet
- Security audit filter

---

## ðŸ“‚ Project Structure

```text
fraud-detection-system/
â”œâ”€â”€ ml/
â”‚   â”œâ”€â”€ models/                  # Model artifacts, if present
â”‚   â””â”€â”€ preprocessing/           # Dataset preparation scripts
â”œâ”€â”€ src/
â”‚   â”œâ”€â”€ main/
â”‚   â”‚   â”œâ”€â”€ java/com/frauddetection/frauddetection/
â”‚   â”‚   â”‚   â”œâ”€â”€ client/          # REST API client
â”‚   â”‚   â”‚   â”œâ”€â”€ config/          # Application and security config
â”‚   â”‚   â”‚   â”œâ”€â”€ controller/      # REST API controllers
â”‚   â”‚   â”‚   â”œâ”€â”€ dto/             # API data transfer objects
â”‚   â”‚   â”‚   â”œâ”€â”€ entity/          # Database entities
â”‚   â”‚   â”‚   â”œâ”€â”€ exception/       # Exception handling
â”‚   â”‚   â”‚   â”œâ”€â”€ fraud/           # Fraud rules and feature extraction
â”‚   â”‚   â”‚   â”œâ”€â”€ gui/             # Swing screens and Java2D charts
â”‚   â”‚   â”‚   â”œâ”€â”€ jdbc/            # Database utilities
â”‚   â”‚   â”‚   â”œâ”€â”€ ml/              # Machine-learning utilities
â”‚   â”‚   â”‚   â””â”€â”€ service/         # Business logic
â”‚   â”‚   â””â”€â”€ resources/
â”‚   â”‚       â”œâ”€â”€ static/          # Static resources
â”‚   â”‚       â””â”€â”€ templates/       # Existing web resources
â”‚   â””â”€â”€ test/
â”‚       â””â”€â”€ java/                # Automated tests
â”œâ”€â”€ design-references/            # UI reference images
â”œâ”€â”€ launch-safepay.bat            # Windows launcher, if present
â”œâ”€â”€ mvnw
â”œâ”€â”€ mvnw.cmd
â””â”€â”€ pom.xml
```

---

## ðŸš€ Getting Started

### ðŸ“‹ Prerequisites

Install the following:

- â˜• JDK version required by `pom.xml` (Java 25 is the project's configured target).
- ðŸ—„ï¸ MySQL 8.
- ðŸªŸ Windows PowerShell or Command Prompt.
- ðŸŒ¿ Git, if cloning the repository.

### 1ï¸âƒ£ Clone the Repository

```bash
git clone https://github.com/Devraj0070/FraudDetection.git
cd FraudDetection
```

### 2. Configure Environment & MySQL

SafePay defaults to port **8081** to avoid port collisions with other common local services (such as Steam on port 8080).

Configure your database password via environment variable (recommended) or in `src/main/resources/application.properties`:

```powershell
$env:DB_PASSWORD = "your_mysql_password"
$env:SERVER_PORT = "8081"
$env:SAFEPAY_API_URL = "http://localhost:8081"
```

Default database: `fraud_detection` on `localhost:3306`. If deploying schema updates to an existing database, run the non-destructive migration script: `src/main/resources/schema-migration.sql`.

> Never commit real database credentials, API keys, or private tokens.

### 3. Build the Application

On Windows:

```powershell
.\mvnw.cmd package -DskipTests
```

### 4. Run Automated Tests

Execute the complete test suite:

```powershell
.\mvnw.cmd test
```

Test reports are generated under: `target/surefire-reports/`.

### 5. Launch SafePay

#### Option A: Interactive Windows Launcher (Recommended)
```powershell
.\launch-safepay.bat
```
Choose `[1]` for Desktop Client, `[2]` for Backend Server, or `[3]` to launch both concurrently.

#### Option B: Dedicated Terminal Commands
1. **Start Backend Server (Port 8081):**
```powershell
$env:DB_PASSWORD = "your_mysql_password"
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

2. **Launch Desktop GUI (Connects to Port 8081):**
```powershell
.\mvnw.cmd exec:java -Dexec.mainClass="com.frauddetection.frauddetection.gui.FraudDetectionGuiApp" -Dexec.args="http://localhost:8081"
```

#### Default Seed Accounts
- **Administrator**: Username: `admin` | Password: `admin123` (Access to fraud alerts, pending review queue, approve/reject)
- **Demo Customer**: Username: `user` | Password: `user123` (Access to balance, transfers, deposits, transaction history)

---

## ðŸ” Security Principles

- 🔑 Keep authentication and authorization enabled.
- 👤 Restrict transaction access to the authorized owner.
- 🛡️ï¸ Validate transaction inputs and access permissions.
- 🔒 Keep passwords and secrets out of source control.
- 🧪 Test fraud rules and security controls before deployment.

> âš ï¸ **Disclaimer:** SafePay is a development project. It is not a certified payment-processing platform. Production use requires additional security testing, reliability validation, privacy controls, and regulatory review.

---

## ðŸ—ºï¸ Future Improvements

Potential areas for future development include:

- ✨ Further polish and animation for the desktop interface.
- 📦 Windows installer packaging.
- 📊 More detailed fraud analytics.
- 🧪 Improved automated UI testing.
- 🔔 Real-time fraud-alert updates.
- 📈 Expanded model evaluation and monitoring.

These are potential improvements, not claims that the features are already implemented.

---

## ðŸ¤ Contributing

Contributions and suggestions are welcome.

1. Create a branch for your changes.
2. Make a focused improvement.
3. Add or update relevant tests.
4. Run the test suite.
5. Open a pull request for review.

---

## 📜 License

A license has not yet been specified here. Add the project's chosen license before allowing reuse under particular terms.

---

<p align="center">
  <b>💳 SafePay — Making Every Transaction Smarter and Safer. 🛡️ï¸</b>
</p>
