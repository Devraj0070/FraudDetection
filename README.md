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
SafePay is an **AI-powered fraud detection system** built with Java. It combines transaction analysis, fraud-risk assessment, machine-learning components, and a desktop interface to help identify potentially suspicious financial activity.

The project uses a **Java Swing and Java2D desktop application** connected to a **Spring Boot REST API**, with MySQL for data storage and Weka for machine-learning functionality.
>>>>>>> origin/main

---

## ✨ Key Features

### 🖥️ Modern Desktop Application
- 🔐 Login and registration interface.
- 📊 Dashboard for transaction and account summaries.
- 💸 Transaction submission interface.
- 📜 Transaction history view.
- 📈 Java2D transaction-amount chart.
- 🛡️ Admin monitoring interface for fraud alerts.

### 🔌 Backend and REST API
- ⚙️ Spring Boot REST API.
- 🔑 Spring Security authentication and authorization.
- 🛡️ CSRF protection for API requests.
- 👤 Transaction ownership checks.
- 📡 API endpoints for authentication, dashboard data, transactions, and administrative operations.
- ⚠️ Centralized exception handling.

### 🤖 Fraud Detection and Machine Learning
- 🔎 Rule-based fraud checks.
- 📉 Transaction feature extraction.
- 🧠 Weka-based machine-learning components.
- 🧪 Dataset loading and preprocessing utilities.
- 📊 Model-training and evaluation utilities where available.

### 🧪 Testing and Reliability
- ✅ Automated tests for backend services and REST controllers.
- 🔐 Authentication and authorization tests.
- 💳 Transaction-processing tests.
- 🧠 Fraud-rule and feature-extraction tests.
- 🖥️ Desktop UI tests.

---

## 🛠️ Technology Stack

<<<<<<< HEAD
The project was developed to demonstrate a complete fraud-detection workflow using Java and related technologies.

### Main objectives

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

---

# 🧰 3. Technology Stack

| Technology | Used For |
|---|---|
| **Java 25** | Main programming language |
| **Spring Boot 4.1.1** | Main backend framework |
| **Spring Security** | Authentication, authorization, CSRF and security controls |
| **Spring Data JPA** | Entity mapping and database persistence |
| **MySQL** | Persistent relational database |
| **Maven** | Build system and dependency management |
| **Weka 3.8.6** | Machine Learning functionality |
| **Random Forest** | Fraud classification model |
| **JDBC** | Direct SQL/database access |
| **Java Servlets** | Metrics, filters and web-level processing |
| **Java Swing** | Desktop fraud-monitoring GUI |
| **JUnit** | Automated testing |


### 🧭 How the main technologies work together

| Technology | Role in SafePay | Simple explanation |
|---|---|---|
| ☕ **Java** | Application code | The language used to write the backend, fraud logic, desktop GUI, and tests. |
| 🌱 **Spring Boot** | Backend application | Starts and configures the web application and connects its components. |
| 🧱 **Maven** | Build and dependencies | Reads `pom.xml`, obtains libraries, compiles code, runs tests, and packages the app. |
| 🐬 **MySQL** | Data storage | Keeps users, accounts, transactions, predictions, and alerts. |
| 🗃️ **Spring Data JPA** | Object/database mapping | Maps Java entities to database records and provides repository operations. |
| 🔌 **JDBC** | Direct SQL access | Supports explicit SQL operations through the project's DAO/service classes. |
| 🔐 **Spring Security + BCrypt** | Web security | Protects routes and verifies securely hashed passwords. |
| 🤖 **Weka + Random Forest** | ML fraud prediction | Loads the trained model and estimates whether a transaction looks fraudulent. |
| 🖥️ **Java Swing** | Desktop tool | Provides the separate monitoring and transaction-simulation GUI. |
| 🌐 **Servlets and filters** | Web-level processing | Provide metrics and security-related request processing. |
| 🧪 **JUnit / test framework** | Quality checks | Tests key parts of the application automatically. |

---

# 💡 4. Why These Technologies Were Chosen

## ☕ 4.1 Java

Java is the core language because the project requires backend development, database access, security, Machine Learning integration, GUI development, and automated testing.

Using Java throughout the major parts of the project also makes the different components easier to integrate.

---

## 🌱 4.2 Spring Boot

Spring Boot is used as the main backend framework.

It provides the infrastructure for:

- HTTP controllers
- Services
- Dependency Injection
- Database integration
- Security
- Configuration
- Testing
- Web application startup

It also gives the project a standard layered backend architecture.

---

## 🔐 4.3 Spring Security

Spring Security is used to protect the API endpoints.

It handles:

- Authentication
- Login
- Role-based authorization
- Password verification
- CSRF protection
- Security headers
- Protected application routes

The application separates normal users from administrators.

---

## 🔑 4.4 BCrypt

BCrypt is used for password hashing.

The application does not need to store the original user password.

The basic process is:
=======
| Technology | Purpose |
|---|---|
| ☕ **Java 25** | Core programming language |
| 🍃 **Spring Boot** | Backend application and REST API |
| 🖥️ **Java Swing** | Native desktop interface |
| 🎨 **Java2D** | Custom charts and desktop graphics |
| 🔐 **Spring Security** | Authentication and authorization |
| 🧠 **Weka** | Machine-learning functionality |
| 🗄️ **MySQL 8** | Relational database |
| 📦 **Maven Wrapper** | Build and dependency management |
| 🧪 **JUnit / Maven Surefire** | Automated testing, where configured |
| 🌿 **Git and GitHub** | Version control and collaboration |

> Check `pom.xml` for the exact dependency versions and test libraries configured in the current source code.

---

## 🏗️ System Architecture
>>>>>>> origin/main

```text
       👤 User / Administrator
                  |
                  v
      🖥️ Java Swing Desktop App
              + Java2D
                  |
                  v
          🔌 REST API Client
                  |
                  v
       🍃 Spring Boot REST API
                  |
          +-------+-------+
          |       |       |
          v       v       v
       🔐 Auth  💳 Fraud  📊 Dashboard
          |       |
          v       v
       🗄️ MySQL  🧠 Weka / Risk Rules
```

The desktop client communicates with the backend through HTTP API requests. The backend handles application logic, security checks, and data access.

---

<<<<<<< HEAD
## 🗃️ 4.6 Spring Data JPA

Spring Data JPA is used to map Java objects to database records.

The main entities include:

- `User`
- `Account`
- `Transaction`
- `FraudPrediction`
- `FraudAlert`

Repository classes provide database operations while keeping persistence code separate from controllers and services.

---

## 🔌 4.7 JDBC

JDBC is also included as a direct database-access layer.

It is used for lower-level database operations and provides DAO/service classes for direct SQL interaction.

This is useful both technically and academically because the project demonstrates both JPA-based persistence and direct JDBC database access.

---

## 🧩 4.8 Thymeleaf

The Java GUI communicates with real Spring Boot components replacing previous HTML.

It allows dynamic information such as:

- Account balance
- Transaction status
- Fraud probability
- Risk level
- Detection reason
- Transaction history

to be displayed in the web interface.

---

## 🤖 4.9 Weka

Weka provides the Java Machine Learning functionality used by SafePay.

It is used to load and work with the trained fraud detection model.

---

## 🌲 4.10 Random Forest

The main fraud prediction model is a **Random Forest** classifier.

Random Forest uses multiple decision trees and combines their predictions.

It was selected because transaction data is structured/tabular data and Random Forest is a practical classification method for this type of problem.

The trained model files are stored under:

```text
ml/models/
```

---

## 🖥️ 4.11 Java Swing

Java Swing is used for a separate desktop monitoring and transaction-simulation application.

It provides another way to demonstrate:

- Fraud detection strategies
- Transaction processing
- Fraud alerts
- Monitoring
- Transaction simulation

---

## 🧱 4.12 Maven and the Maven Wrapper

**Maven is the project's build tool.** It is not the programming language or the backend framework. Java contains the application code, Spring Boot runs the application, and Maven helps build the project and manage the libraries they need.

Maven is used to:

- 📦 Download and manage dependencies declared in `pom.xml`.
- 🏗️ Compile the Java source code.
- 🧪 Run automated tests.
- 📦 Package the application into a build artifact.
- 🔁 Run repeatable build tasks using standard commands.

The repository includes `mvnw` and `mvnw.cmd`. These are the **Maven Wrapper** scripts. On Windows, use `mvnw.cmd` from the project root, so you do not need to install a separate Maven version just to run the project's build.

Common Windows commands:

```powershell
# Run the application
.\mvnw.cmd spring-boot:run

# Compile and run the tests
.\mvnw.cmd clean test

# Build the project
.\mvnw.cmd clean package
```

The `pom.xml` file defines project metadata, dependencies, and Maven build configuration.

---

# 🏗️ 5. High-Level Architecture

SafePay follows a layered architecture.

```text
                         USER / ADMIN
                              |
                              v
                    +--------------------+
                    |    Web Interface   |
                    | HTML + Thymeleaf   |
                    +---------+----------+
                              |
                              v
                    +--------------------+
                    |    Controllers     |
                    +---------+----------+
                              |
                              v
                    +--------------------+
                    |      Services      |
                    +---------+----------+
                              |
               +--------------+--------------+
               |              |              |
               v              v              v
        +-------------+ +-------------+ +-------------+
        | ML Predictor| | Rule Engine | | Account/DB  |
        +------+------+ +------+------+ +------+------+
               |              |              |
               +--------------+--------------+
                              |
                              v
                    +--------------------+
                    | Security Decision  |
                    +---------+----------+
                              |
              +---------------+---------------+
              |               |               |
              v               v               v
          APPROVED         DECLINED        BLOCKED
                              |
                              v
                         +---------+
                         |  MySQL  |
                         +---------+
```

---

# 🔄 6. Complete Transaction Flow

When a user submits a transaction, the application processes it in stages.

```text
User submits transaction
          |
          v
Validate amount
          |
          v
Validate transaction type
          |
          v
Load account
          |
          v
Run Machine Learning prediction
          |
          v
Run behavioral/rule analysis
          |
          +---- Suspicious -----------------> BLOCKED
          |
          v
Check transaction limit
          |
          +---- Limit exceeded ------------> DECLINED
          |
          v
Check account balance
          |
          +---- Insufficient balance ------> DECLINED
          |
          v
Process transaction
          |
          v
Update account balance
          |
          v
APPROVED
```

The important security rule is:

> A blocked transaction must not modify the account balance.

---

# 🚨 7. Fraud Detection Design

SafePay uses two main fraud-analysis layers.

## 7.1 Machine Learning Detection

The Random Forest model analyzes transaction features and produces a prediction.

The result contains information such as:

```text
Prediction: LEGITIMATE / FRAUD
Fraud Probability: 0% - 100%
```

The ML prediction represents the output of the trained statistical model.

---

## 7.2 Rule-Based Detection

The `FraudRuleEngine` performs deterministic analysis.

It examines recent transaction behavior and identifies suspicious patterns such as:

- Multiple transactions in a short time.
- Repeated large transactions.
- Large transactions combined with recent activity.
- Very large transactions combined with previous activity.
- High transaction frequency.

This approach is useful because the result can be explained directly.

For example:

```text
Detection Reason:
Behavioral Anomaly / Suspicious Activity
```

---

# 🧠 8. Why Both ML and Rules Are Used

Machine Learning and deterministic rules solve different problems.

### Machine Learning

Useful for:

- Learning patterns from historical data.
- Producing a probability-based prediction.
- Detecting statistical relationships.

### Rules

Useful for:

- Explicit security requirements.
- Business constraints.
- Explainable behavioral patterns.
- Deterministic decisions.

Therefore:

```text
Machine Learning
        +
Behavioral Rules
        +
Transaction Limits
        +
Balance Validation
        =
Complete Transaction Analysis
```

---

# ⚖️ 9. ML Prediction vs Final Security Decision

SafePay intentionally keeps these two concepts separate.

Example:

```text
ML Prediction:
LEGITIMATE

ML Fraud Probability:
0.00%

Behavioral Analysis:
SUSPICIOUS

Final Security Decision:
BLOCKED

Detection Reason:
Behavioral Anomaly
```

This is valid because the ML model and behavioral engine are separate detection layers.

The final decision is based on the complete security analysis.

This design also prevents the result page from showing misleading information such as:

```text
BLOCKED
VERIFIED LEGITIMATE
```

Instead, the UI displays each detection dimension separately.

---

# 📊 10. Risk Assessment

The system also calculates/displays a risk assessment.

The main risk levels are:

```text
LOW
MEDIUM
HIGH
```

A blocked transaction is treated as high risk even when the ML prediction itself is legitimate.

This makes the final result consistent with the security decision.

---

# 💳 11. Transaction Types

SafePay supports five transaction types:

| Type | Meaning |
|---|---|
| `PAYMENT` | Payment transaction |
| `TRANSFER` | Transfer of funds |
| `CASH_OUT` | Cash withdrawal/cash-out |
| `DEBIT` | Debit transaction |
| `CASH_IN` | Cash deposit/cash-in |

Each type has an application-defined demonstration/business limit.

These values are SafePay application rules and are not intended to represent universal banking limits.

---

# 🚦 12. Transaction Decision Logic

## APPROVED

A transaction is approved when:

- The input is valid.
- Fraud analysis does not block it.
- The amount is within the configured transaction limit.
- The account has enough balance.

For normal debit-type transactions, the account balance is reduced.

For `CASH_IN`, the account balance is increased.

---

## DECLINED

A transaction is declined when it is otherwise valid but cannot be processed.

Examples:

### Limit Exceeded

```text
Amount > Allowed Transaction Limit
```

### Insufficient Balance

```text
Required Amount > Available Balance
```

A declined transaction is not automatically classified as fraud.

---

## BLOCKED

A transaction is blocked when fraud/security analysis identifies suspicious activity.

A blocked transaction:

- Does not modify the account balance.
- Can generate a fraud alert.
- Is displayed as high risk.
- Shows the relevant detection reason.

---

# 🗄️ 13. Database Model

The main domain objects are:

```text
User
 |
 +---- Account
          |
          +---- Transaction
                    |
                    +---- FraudPrediction
                    |
                    +---- FraudAlert
```

### User

Represents a registered application user.

### Account

Represents the user's financial account and balance.

### Transaction

Represents a financial operation submitted through SafePay.

### FraudPrediction

Stores information related to fraud prediction.

### FraudAlert

Stores fraud-related alerts.

---

# 🛡️ 14. Security Architecture

```text
                User
                 |
                 v
          Authentication
                 |
                 v
        Spring Security
                 |
       +---------+---------+
       |         |         |
       v         v         v
    BCrypt     Roles     CSRF
       |         |         |
       +---------+---------+
                 |
                 v
             Application
```

Security-related components include:

```text
config/
    PasswordConfig.java
    SecurityConfig.java

security/
    CustomUserDetailsService.java

web/
    FraudSecurityAuditFilter.java
```

---

# 👤 15. Authentication and Authorization

The application supports at least two logical roles:

## USER

Normal users can:

- Register.
- Log in.
- View their dashboard.
- Submit transactions.
- View transaction results.
- View transaction history.

## ADMIN

Administrators have additional access to administrative functionality such as fraud alerts.

Protected administrative endpoints cannot be accessed by normal users.

---

# 🧱 16. CSRF and Security Headers

Spring Security provides CSRF protection for the web application.



The project therefore does not treat fraud detection as only a Machine Learning problem; application security is also part of the system.

---

# 🧱 17. MVC / Layered Application Structure

The main Spring Boot application is divided into packages according to responsibility.

```text
com.frauddetection.frauddetection/

+-- config/
+-- controller/
+-- entity/
+-- exception/
+-- fraud/
+-- gui/
+-- jdbc/
+-- ml/
+-- repository/
+-- security/
+-- service/
+-- web/
```

---

# ⚙️ 18. Configuration Package

```text
config/
+-- DataInitializer.java
+-- PasswordConfig.java
+-- SecurityConfig.java
```

### DataInitializer

Used for application initialization/data setup.

### PasswordConfig

Contains password-related configuration.

### SecurityConfig

Defines application security behavior, authentication/authorization rules, CSRF and security settings.

---

# 🎮 19. Controller Package

```text
controller/
+-- AdminController.java
+-- AuthController.java
+-- DashboardController.java
+-- TransactionController.java
```

Controllers receive requests from the web interface.

They coordinate with the service layer instead of putting all business logic directly into the HTML layer.

---

# 📦 20. Entity Package

```text
entity/
+-- Account.java
+-- FraudAlert.java
+-- FraudPrediction.java
+-- Transaction.java
+-- TransactionType.java
+-- User.java
```

These classes represent the main application data/domain objects.

They are used with the persistence layer to store and retrieve application data.

---

# ⚙️ 21. Service Package

```text
service/
+-- TransactionService.java
+-- UserService.java
```

The service layer contains business logic.

`TransactionService` coordinates important transaction operations such as:

- Validation
- Account lookup
- ML prediction
- Rule analysis
- Limit checks
- Balance checks
- Balance updates
- Transaction status
- Fraud-related information

---

# 🗂️ 22. Repository Package

```text
repository/
+-- AccountRepository.java
+-- FraudAlertRepository.java
+-- FraudPredictionRepository.java
+-- TransactionRepository.java
+-- UserRepository.java
```

Repositories provide persistence operations and keep database-access logic separate from controllers.

---

# 🚨 23. Fraud Package

The fraud package contains the core fraud-detection components.

```text
fraud/
|
+-- FraudRuleEngine.java
+-- RiskLevel.java
+-- RiskScoreCalculator.java
|
+-- feature/
|   +-- FeatureExtractor.java
|   +-- FeatureVector.java
|   +-- TransactionFeatureExtractor.java
|
+-- prediction/
    +-- FraudPredictionResult.java
    +-- FraudPredictionService.java
    +-- FraudPredictor.java
    +-- RandomForestPredictor.java
```

The package is separated into:

- Rule detection
- Risk calculation
- Feature extraction
- ML prediction

---

# 🧪 24. Feature Extraction

Before Machine Learning prediction, transaction information must be represented in a form the model can use.

The project contains:

```text
FeatureExtractor.java
FeatureVector.java
TransactionFeatureExtractor.java
```

These components are responsible for extracting and organizing transaction-related features.

---

# 🤖 25. Machine Learning Package

The Java-side ML components are:

```text
ml/
|
+-- evaluation/
|   +-- ModelEvaluator.java
|
+-- model/
|   +-- ModelManager.java
|
+-- training/
    +-- DatasetLoader.java
    +-- ModelTrainer.java
```

### DatasetLoader

Loads training/data resources.

### ModelTrainer

Provides model-training functionality.

### ModelEvaluator

Provides model evaluation functionality.

### ModelManager

Manages trained model handling.

---

# 🌲 26. Trained Models

The trained models are stored outside the Java package structure:

```text
ml/models/
+-- fraud_random_forest.model
+-- fraud_random_forest_final.model
```

Keeping the model files in a dedicated `ml/models` directory makes it clear that they are trained artifacts rather than Java source code.

---

# 🔬 27. Machine Learning Data Pipeline

The ML workflow is:

```text
Source Transaction Data
          |
          v
Data Preparation
          |
          v
Preprocessing
          |
          v
Dataset Splitting
          |
          +---- Training
          |
          +---- Validation
          |
          +---- Testing
          |
          v
Random Forest
          |
          v
Evaluation
          |
          v
Saved Model
          |
          v
SafePay Application
          |
          v
Transaction Prediction
```

The repository contains dataset-generation and preprocessing scripts under `ml/dataset` and `ml/preprocessing`.

---

# 📚 28. ML Dataset Structure

```text
ml/dataset/
+-- fraud_test.csv
+-- fraud_train.csv
+-- fraud_transactions.csv
+-- fraud_transactions_generated.csv
+-- fraud_validation.csv
+-- generate_dataset.py
```

The project separates training, validation, and testing data to support model development and evaluation.

---

# 🧹 29. ML Preprocessing

```text
ml/preprocessing/
+-- create_fraud_dataset_splits.py
+-- create_realistic_test.py
+-- prepare_paysim.py
+-- split_paysim.py
```

These scripts support preparation and splitting of transaction data before model training/evaluation.

---

# 🖥️ 30. Java Swing Monitoring Application

The project also contains a desktop GUI:

```text
gui/
+-- BaseAppFrame.java
+-- FraudAlertObserver.java
+-- FraudDetectionGuiApp.java
+-- FraudDetectionStrategy.java
+-- FraudMonitorFrame.java
+-- GenericTableModel.java
+-- GuiException.java
+-- HybridStrategy.java
+-- MachineLearningStrategy.java
+-- RuleBasedStrategy.java
+-- TransactionProcessListener.java
+-- TransactionSimulationDialog.java
+-- TransactionSimulationWorker.java
+-- ValidationException.java
```

The GUI provides an additional environment for:

- Fraud monitoring
- Transaction simulation
- Fraud strategy demonstration
- Fraud alerts
- Transaction processing
- Data-table presentation

---

# 🧠 31. Strategy Pattern

The GUI contains multiple fraud-detection strategies:

```text
FraudDetectionStrategy
        |
        +-- RuleBasedStrategy
        |
        +-- MachineLearningStrategy
        |
        +-- HybridStrategy
```

The Strategy pattern allows different fraud-detection approaches to be represented independently.

This avoids putting every detection approach into one large class.

---

# 👀 32. Observer Pattern

The project also contains:

```text
FraudAlertObserver
```

The observer approach allows monitoring components to react to fraud-related events without tightly coupling every part of the application together.

---

# 🔌 33. JDBC Architecture

The JDBC package contains:

```text
jdbc/
+-- DatabaseConnection.java
+-- DatabaseOperationException.java
+-- DatabaseOperations.java
+-- FraudAnalyticsSummary.java
+-- FraudJdbcService.java
+-- TransactionJdbcDao.java
+-- TransactionRecord.java
+-- UserJdbcDao.java
+-- UserRecord.java
```

The JDBC layer provides direct database access and DAO-style operations.

It complements the JPA repository layer and demonstrates direct SQL-based persistence.

---

# 🌐 34. Web / Servlet Layer

The project contains:

```text
web/
+-- FraudMetricsServlet.java
+-- FraudSecurityAuditFilter.java
+-- ServletConfig.java
```

These components provide servlet-level functionality for metrics, filtering, configuration, and security-related processing.

---

# 🧯 35. Exception Handling

The exception package contains:

```text
exception/
+-- ApplicationException.java
+-- GlobalExceptionHandler.java
+-- TransactionException.java
```

The purpose is to handle errors in a controlled manner and provide appropriate application responses instead of exposing raw internal errors.

---

# 🎨 36. Frontend Structure

The frontend is stored under:

```text
src/main/resources/
|
+-- static/
|   +-- css/
|   +-- images/
|
+-- templates/
    +-- admin/
    +-- auth/
    +-- error/
    +-- user/
```

### CSS

The project separates styling by feature:

```text
static/css/
+-- admin.css
+-- auth.css
+-- dashboard.css
+-- error.css
+-- global.css
+-- history.css
+-- result.css
+-- transaction.css
```

### Images

```text
static/images/
+-- safepay-flow.svg
+-- safepay-logo.png
```

### Thymeleaf templates

```text
templates/
+-- admin/
|   +-- alerts.html
|
+-- auth/
|   +-- login.html
|   +-- register.html
|
+-- error/
|   +-- 403.html
|
+-- user/
    +-- dashboard.html
    +-- history.html
    +-- result.html
    +-- transaction.html
```

---

# 🧭 37. User Interface Flow

The main user journey is:

```text
Register / Login
       |
       v
Dashboard
       |
       v
Transaction Page
       |
       v
Submit Transaction
       |
       v
Fraud Analysis
       |
       v
Result Page
       |
       v
Transaction History
```

The result page displays the important detection information separately:

- ML prediction
- ML fraud probability
- Final security decision
- Detection reason
- Risk assessment
- Transaction status

---

# 🔁 38. Database and Application Interaction

The complete backend flow is:

```text
Browser
   |
   v
Controller
   |
   v
Service
   |
   +------> Fraud / ML Analysis
   |
   +------> Repository / JDBC
               |
               v
             MySQL
```

The service layer coordinates the transaction while repositories/JDBC components handle persistence.

---

# 🧪 39. Testing Architecture

The project contains automated tests under:

```text
src/test/java/com/frauddetection/frauddetection/
```

The test categories include:

```text
controller/
fraud/
gui/
jdbc/
ml/
security/
service/
web/
```

Examples from the project include tests for:

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

This gives the project automated coverage across several architectural layers.

---

# 🗂️ 40. Project Structure
=======
## 📂 Project Structure
>>>>>>> origin/main

```text
fraud-detection-system/
├── ml/
│   ├── models/                  # Model artifacts, if present
│   └── preprocessing/           # Dataset preparation scripts
├── src/
│   ├── main/
│   │   ├── java/com/frauddetection/frauddetection/
│   │   │   ├── client/          # REST API client
│   │   │   ├── config/          # Application and security config
│   │   │   ├── controller/      # REST API controllers
│   │   │   ├── dto/             # API data transfer objects
│   │   │   ├── entity/          # Database entities
│   │   │   ├── exception/       # Exception handling
│   │   │   ├── fraud/           # Fraud rules and feature extraction
│   │   │   ├── gui/             # Swing screens and Java2D charts
│   │   │   ├── jdbc/            # Database utilities
│   │   │   ├── ml/              # Machine-learning utilities
│   │   │   └── service/         # Business logic
│   │   └── resources/
│   │       ├── static/          # Static resources
│   │       └── templates/       # Existing web resources
│   └── test/
│       └── java/                # Automated tests
├── design-references/            # UI reference images
├── launch-safepay.bat            # Windows launcher, if present
├── mvnw
├── mvnw.cmd
└── pom.xml
```

---

## 🚀 Getting Started

### 📋 Prerequisites

Install the following:

- ☕ JDK version required by `pom.xml` (Java 25 is the project's configured target).
- 🗄️ MySQL 8.
- 🪟 Windows PowerShell or Command Prompt.
- 🌿 Git, if cloning the repository.

### 1️⃣ Clone the Repository

```bash
git clone https://github.com/Devraj0070/FraudDetection.git
cd FraudDetection
```

### 2️⃣ Configure MySQL

Open:

```text
src/main/resources/application.properties
```

Configure the database URL, username, and password for your local environment. Create the required database and schema according to the project's setup.

🔒 **Never commit real database credentials, API keys, or private tokens.**

### 3️⃣ Build the Application

On Windows:

```powershell
.\mvnw.cmd clean package
```

### 4️⃣ Run Automated Tests

```powershell
.\mvnw.cmd test
```

📄 Test reports are generated under:

```text
target/surefire-reports/
```

Review any test failures before treating the build as verified.

### 5️⃣ Launch SafePay

If the Windows launcher is available, run:

```powershell
.\launch-safepay.bat
```

Follow the launcher's prompts to start the required application components.

---

## 🔐 Security Principles

- 🔑 Keep authentication and authorization enabled.
- 👤 Restrict transaction access to the authorized owner.
- 🛡️ Validate transaction inputs and access permissions.
- 🔒 Keep passwords and secrets out of source control.
- 🧪 Test fraud rules and security controls before deployment.

> ⚠️ **Disclaimer:** SafePay is a development project. It is not a certified payment-processing platform. Production use requires additional security testing, reliability validation, privacy controls, and regulatory review.

---

## 🗺️ Future Improvements

Potential areas for future development include:

- ✨ Further polish and animation for the desktop interface.
- 📦 Windows installer packaging.
- 📊 More detailed fraud analytics.
- 🧪 Improved automated UI testing.
- 🔔 Real-time fraud-alert updates.
- 📈 Expanded model evaluation and monitoring.

These are potential improvements, not claims that the features are already implemented.

---

## 🤝 Contributing

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
  <b>💳 SafePay — Making Every Transaction Smarter and Safer. 🛡️</b>
</p>
