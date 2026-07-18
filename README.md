# KYC Service

A **Know Your Customer (KYC)** microservice responsible for document verification and identity validation.

The service receives user-submitted documents, stores them securely, performs automated OCR-based validation, and routes submissions requiring manual review to authorized analysts.

> 🚧 **Project Status:** This project is currently **under active development**. Features, endpoints, and workflows may change as development progresses.

**Default Port:** `8083`

---

# Features

- 📄 Upload and securely store documents using MinIO
- 🔍 Perform OCR processing asynchronously without blocking client requests
- 🤖 Automatically approve or flag documents for manual review
- 👨‍💼 Separate APIs for customers and KYC analysts
- 📜 Maintain a complete audit trail of status changes
- 🔐 Authenticate requests using JWT (shared with the Authentication Service)

---

# Technology Stack

- Java 21
- Spring Boot 4
- Spring Security OAuth2 Resource Server
- MySQL
- MinIO Object Storage
- Tess4J (Tesseract OCR)
- Apache PDFBox

---

# Prerequisites

## Install Tesseract OCR

Ubuntu/Debian:

```bash
sudo apt install tesseract-ocr tesseract-ocr-por
```

---

## Start MinIO

```bash
docker run -d \
  -p 9000:9000 \
  -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  quay.io/minio/minio server /data --console-address ":9001"
```

---

# Configuration

```bash
export DB_URL=jdbc:mysql://localhost:3306/kyc_db
export DB_USER=root
export DB_PASS=password

export JWT_SECRET_BASE64=your_auth_service_secret

export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
```

---

# API

## Customer Endpoints

Base path:

```
/kyc/submissions
```

Accessible to any authenticated user.

| Method | Endpoint | Description |
|---------|----------|-------------|
| POST | `/kyc/submissions` | Submit a document |
| GET | `/kyc/submissions` | List the authenticated user's submissions |
| GET | `/kyc/submissions/{id}` | Retrieve submission details |

### Upload Example

```bash
curl -X POST http://localhost:8083/kyc/submissions \
  -H "Authorization: Bearer <token>" \
  -F "file=@passport.jpg" \
  -F "documentType=PASSPORT"
```

---

## Analyst Endpoints

Base path:

```
/kyc/analyst
```

Requires one of the following roles:

- `ROLE_KYC_ANALYST`
- `ROLE_SUPERADMIN`

| Method | Endpoint | Description |
|---------|----------|-------------|
| GET | `/submissions` | Paginated list with filters |
| GET | `/submissions/{id}` | Submission details including OCR results |
| GET | `/submissions/{id}/document-url` | Generate a temporary document URL |
| POST | `/submissions/{id}/decision` | Approve or reject a submission |
| GET | `/submissions/{id}/history` | Retrieve status history |
| GET | `/metrics` | Dashboard metrics grouped by status and document type |

---

## Filtering

Example:

```http
GET /kyc/analyst/submissions?status=MANUAL&documentType=PASSPORT&page=0&size=20
```

---

## Decision Request

```json
{
  "action": "REJECT",
  "rejectionReason": "DOCUMENT_UNREADABLE",
  "note": "The uploaded image is out of focus."
}
```

Supported actions:

- `APPROVE`
- `REJECT`

---

# Workflow

```
NEW
   │
   ▼
IN_PROGRESS
   │
   ├────────────► APPROVED
   │
   ▼
MANUAL
   │
   ├────────────► APPROVED
   │
   └────────────► REJECTED
```

Customers only have access to the final verification status.

Raw OCR output and internal analysis are only available to authorized analysts.

---

# Running the Service

```bash
./mvnw spring-boot:run
```

The **Authentication Service** must be running before starting this application, since JWT tokens are validated using the same shared secret.

---

# Architecture

```
                Client
                   │
                   ▼
             KYC Service
                   │
     ┌─────────────┴─────────────┐
     │                           │
     ▼                           ▼
   MinIO                    OCR Engine
(File Storage)         (Tesseract + PDFBox)
     │                           │
     └─────────────┬─────────────┘
                   ▼
                MySQL
```

---

# Future Improvements

Some planned features include:

- Document expiration validation
- Face matching (Selfie × ID) -> I don't see this as a god idea.
- Liveness detection
- Support for additional document types -> Doing
- Multi-language OCR -> Doing
- AI-assisted document classification -> Badass move—I'll make it a priority
- Integration with cloud storage providers (Amazon S3, Azure Blob Storage) -> Useless at the moment
- Event-driven processing using RabbitMQ/Kafka -> useless at the moment
