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

SafePay is an **AI-powered fraud detection system** built with Java. It combines transaction analysis, fraud-risk assessment, machine-learning components, and a desktop interface to help identify potentially suspicious financial activity.

The project uses a **Java Swing and Java2D desktop application** connected to a **Spring Boot REST API**, with MySQL for data storage and Weka for machine-learning functionality.

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

## 📂 Project Structure

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
