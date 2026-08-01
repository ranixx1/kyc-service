# kyc-service

A **Know Your Customer (KYC)** microservice for document verification and identity validation. Receives user-submitted documents, stores them securely in MinIO, runs automated OCR-based analysis, and routes submissions requiring human review to authorized analysts.

> **Status:** Active development — features and endpoints may change.

**Default port:** `8083`

---

## Features

- Upload and store documents securely via MinIO
- Asynchronous OCR processing — client receives an immediate response
- Automated field extraction per document type
- Rule-based validation engine — extensible without modifying existing code
- Separate APIs for customers and KYC analysts
- Complete audit trail of every status change
- JWT authentication shared with the Authentication Service

---

## Stack

- Java 21 + Spring Boot 4
- Spring Security OAuth2 Resource Server
- MySQL
- MinIO (object storage)
- Tess4J / Tesseract OCR
- Apache PDFBox

---

## Supported Document Types

| Type | Description |
|------|-------------|
| `ID_CARD` | National identity card |
| `DRIVER_LICENSE` | Driver's license |
| `PASSPORT` | International passport |
| `BANK_STATEMENT` | Bank account statement |
| `PAY_SLIP` | Salary or pay slip |
| `UTILITY_BILL` | Electricity, water or gas bill |
| `PHONE_BILL` | Mobile or landline bill |

---

## Prerequisites

**Tesseract OCR**
```bash
sudo apt install tesseract-ocr tesseract-ocr-por tesseract-ocr-eng
```

**MinIO**
```bash
docker run -d \
  -p 9000:9000 \
  -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  quay.io/minio/minio server /data --console-address ":9001"
```

The MinIO console is available at `http://localhost:9001`.

---

## Configuration

```bash
export DB_URL=jdbc:mysql://localhost:3306/kyc_db
export DB_USER=root
export DB_PASS=password
export JWT_SECRET_BASE64=your_shared_secret
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
```

The `JWT_SECRET_BASE64` must match the secret used by the Authentication Service.

---

## Running

```bash
./mvnw spring-boot:run
```

The Authentication Service must be running before this service starts.

---

## Running Tests

```bash
./mvnw test
```

All unit tests run without MinIO, MySQL, or Tesseract installed.

---

## API

### Customer Endpoints — `/kyc/submissions`

Any authenticated user. Each user only sees their own submissions.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/kyc/submissions` | Submit a document for verification |
| `GET` | `/kyc/submissions` | List own submissions |
| `GET` | `/kyc/submissions/{id}` | Submission details |

**Upload example:**
```bash
curl -X POST http://localhost:8083/kyc/submissions \
  -H "Authorization: Bearer <token>" \
  -F "file=@passport.jpg" \
  -F "documentType=PASSPORT"
```

Accepted formats: `JPG`, `PNG`, `PDF` — maximum 10 MB.

---

### Analyst Endpoints — `/kyc/analyst`

Requires `ROLE_KYC_ANALYST`, `ROLE_ADMIN`, or `ROLE_SUPERADMIN`.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/kyc/analyst/submissions` | Paginated list with filters |
| `GET` | `/kyc/analyst/submissions/{id}` | Full details including OCR text and extracted fields |
| `GET` | `/kyc/analyst/submissions/{id}/document-url` | Temporary URL to view the document (15 min) |
| `POST` | `/kyc/analyst/submissions/{id}/decision` | Approve or reject |
| `GET` | `/kyc/analyst/submissions/{id}/history` | Full status history |
| `GET` | `/kyc/analyst/metrics` | Totals grouped by status and document type |

**Filtering:**
```
GET /kyc/analyst/submissions?status=MANUAL&documentType=PASSPORT&page=0&size=20&sortBy=createdAt&sortDir=asc
```

**Decision payload:**
```json
{
  "action": "REJECT",
  "rejectionReason": "EXPIRED_DOCUMENT",
  "note": "Document expired on 01/01/2023."
}
```

Supported actions: `APPROVE`, `REJECT`.

---

## Submission Status Flow

```
NEW
 │
 ├─ OCR passed ──► IN_PROGRESS ──► APPROVED
 │                                 REJECTED
 │
 └─ OCR failed ──► MANUAL ──────► APPROVED
                                   REJECTED
```

Customers see only the terminal status. OCR text, extracted fields, and validation errors are visible only to analysts.

---

## Architecture

The service follows a layered pipeline. Each stage is independent and replaceable.

```
Upload
  │
  ▼
MinIO (storage)
  │
  ▼
OcrProvider (Tesseract)
  │
  ▼
DocumentAnalyzer
  ├── DocumentExtractor   → typed field extraction per document type
  └── ValidationEngine   → rule-based validation (one rule = one class)
  │
  ▼
Decision (IN_PROGRESS or MANUAL)
  │
  ▼
MySQL + Audit Trail
```

**Adding a new document type** requires:
1. A value in the `DocumentType` enum with `expectedPatterns()`
2. A typed model implementing `ExtractedDocument`
3. A `DocumentExtractor` implementation annotated with `@Component`
4. Any applicable `ValidationRule` implementations annotated with `@Component`

No other changes are required — Spring auto-discovers all components.

---

## Project Structure

```
src/main/java/com/example/kyc_service/
├── config/
├── controller/
│   ├── KycClientController.java
│   └── KycAnalystController.java
├── dto/
├── enums/
│   ├── DocumentType.java
│   ├── RejectionReason.java
│   └── SubmissionStatus.java
├── exception/
├── model/
│   ├── KycSubmission.java
│   ├── KycStatusHistory.java
│   ├── JsonMapConverter.java
│   └── JsonListConverter.java
├── repository/
├── service/
│   ├── KycSubmissionService.java
│   ├── KycOcrProcessor.java
│   ├── analysis/
│   │   ├── DocumentAnalyzer.java
│   │   ├── DocumentAnalysis.java
│   │   ├── document/         ← typed document models
│   │   ├── extractor/        ← per-type field extractors
│   │   └── validation/       ← rule engine + rules
│   └── ocr/
│       ├── OcrProvider.java  ← interface
│       └── TesseractOcrProvider.java
└── storage/
    └── MinioStorageService.java
```

---

## Roadmap

- [x] OCR separation from business logic
- [x] Per-type field extraction
- [x] Rule-based validation engine
- [ ] Validation score (0–100)
- [ ] Fraud detection layer
- [ ] Automatic document classification
- [ ] AI-assisted OCR correction
