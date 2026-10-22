# Contract Manager (Java / Spring Boot)

Upload a contract (PDF / DOC / DOCX), store it, move it through **Draft → Review → Approved**, and ask basic questions about it.

## Run
Requires JDK 17+ and Maven.

    mvn spring-boot:run

Open http://localhost:8080 for the UI. The API is under `/api/contracts`.
Data is saved in `./data` (H2 file DB) and original files in `./uploads`.

## API
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/contracts` | Create: multipart `file` (+ optional `title`). Parses and stores it as DRAFT |
| GET | `/api/contracts` | List contracts (metadata) |
| GET | `/api/contracts/{id}` | Fetch contract with parsed text |
| PATCH | `/api/contracts/{id}/status` | Body `{"status":"REVIEW"}`. Forward only: DRAFT→REVIEW→APPROVED |
| POST | `/api/contracts/{id}/ask` | Body `{"question":"..."}`. Keyword Q&A |
| GET | `/api/contracts/{id}/download` | Download the original file |

## curl examples
    curl -F "file=@contract.pdf" -F "title=NDA" http://localhost:8080/api/contracts
    curl http://localhost:8080/api/contracts/1
    curl -X PATCH -H "Content-Type: application/json" -d '{"status":"REVIEW"}' http://localhost:8080/api/contracts/1/status
    curl -X POST -H "Content-Type: application/json" -d '{"question":"What is the payment term?"}' http://localhost:8080/api/contracts/1/ask

## How it works
- **Parsing**: Apache PDFBox (PDF), Apache POI (DOCX / DOC).
- **Q&A**: the text is split into sentences; question keywords (stop-words removed, lightly stemmed) are weighted by rarity (IDF) and the best-scoring sentences are returned.
- **Status**: invalid jumps (e.g. DRAFT→APPROVED) return `409 Conflict` with the next allowed status.
