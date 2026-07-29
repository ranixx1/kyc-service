# KYC Service

A **Know Your Customer (KYC)** microservice responsible for document verification and identity validation.

The service receives user-submitted documents, stores them securely in **MinIO**, performs **OCR-based analysis** using **Tesseract**, and routes submissions requiring manual review to authorized analysts.

> 🚧 **Project Status:** In development.

---

# Features

- Upload identity documents
- Secure document storage with MinIO
- OCR extraction using Tesseract
- Automatic document validation
- Manual review workflow
- Submission history tracking
- Analyst dashboard
- JWT authentication
- REST API for customers and analysts

---

# Supported Document Types

| Document | Enum |
|----------|------|
| Identity Card | `ID_CARD` |
| Driver License | `DRIVER_LICENSE` |
| Passport | `PASSPORT` |
| Bank Statement | `BANK_STATEMENT` |
| Pay Slip | `PAY_SLIP` |
| Utility Bill | `UTILITY_BILL` |
| Phone Bill | `PHONE_BILL` |

---

# Tech Stack

- Java 21
- Spring Boot
- Spring Security (OAuth2 Resource Server)
- Spring Data JPA
- MySQL
- MinIO
- Tess4J (Tesseract OCR)
- Apache PDFBox
- Maven

---

# Architecture

```
Client
   │
   ▼
REST API
   │
   ├── Authentication (JWT)
   ├── Upload
   ├── OCR Analysis
   ├── Validation
   ├── Manual Review
   └── MinIO Storage
```

---

# Project Structure

```
src
├── config
├── controller
├── dto
├── enums
├── exception
├── model
├── repository
├── security
├── service
│   ├── analysis
│   ├── ocr
│   └── storage
└── test
```

---

# Requirements

- Java 21+
- Maven 3.9+
- Docker
- Docker Compose
- Tesseract OCR

Ubuntu:

```bash
sudo apt install tesseract-ocr
sudo apt install tesseract-ocr-por
```

---

# Running with Docker

Start MinIO and other services:

```bash
docker compose up -d
```

---

# Configuration

Configure the application using environment variables or your local `application.properties`.

Example:

```properties
spring.datasource.url=...
spring.datasource.username=...
spring.datasource.password=...

minio.endpoint=http://localhost:9000
minio.access-key=********
minio.secret-key=********

spring.security.oauth2.resourceserver.jwt.secret-key=********
```

> **Important:** Never commit secrets or production credentials.

---

# Running the Application

```bash
./mvnw spring-boot:run
```

Default port:

```
8083
```

---

# API Overview

## Customer

| Method | Endpoint | Description |
|---------|----------|-------------|
| POST | `/kyc/submissions` | Submit a document |
| GET | `/kyc/submissions` | List user submissions |
| GET | `/kyc/submissions/{id}` | Submission details |

---

## Analyst

| Method | Endpoint | Description |
|---------|----------|-------------|
| GET | `/kyc/analyst/submissions` | List submissions |
| GET | `/kyc/analyst/submissions/{id}` | Submission details |
| GET | `/kyc/analyst/submissions/{id}/document-url` | Temporary document URL |
| POST | `/kyc/analyst/submissions/{id}/decision` | Approve or reject |
| GET | `/kyc/analyst/submissions/{id}/history` | Submission history |
| GET | `/kyc/analyst/metrics` | Dashboard metrics |

---

# Upload Example

```bash
curl -X POST http://localhost:8083/kyc/submissions \
-H "Authorization: Bearer <token>" \
-F "file=@passport.jpg" \
-F "documentType=PASSPORT"
```

---

# Security

The service acts as an **OAuth2 Resource Server** and validates JWT access tokens.

Authorization is role-based.

Supported roles include:

- `ROLE_USER`
- `ROLE_KYC_ANALYST`
- `ROLE_SUPERADMIN`

---

# Testing

Run all tests:

```bash
./mvnw test
```

Current coverage includes:

- OCR analysis
- Document validation
- Service layer
- Unit tests

> Integration tests for MinIO and OCR are planned.

---

# Future Improvements

- Face Match (Selfie × Document)
- Liveness Detection
- Event-driven OCR processing
- RabbitMQ / Kafka integration
- Amazon S3 support
- Azure Blob Storage support
- Testcontainers integration
- OCR confidence improvements

---

# License

This project is intended for educational and portfolio purposes.
