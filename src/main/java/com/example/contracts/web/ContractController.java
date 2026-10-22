package com.example.contracts.web;

import com.example.contracts.model.Contract;
import com.example.contracts.model.ContractStatus;
import com.example.contracts.service.ContractService;
import com.example.contracts.service.QaService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    /** 1. Create contract: multipart form with "file" (PDF/DOC/DOCX) and optional "title". */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Dto.ContractSummary> create(@RequestPart("file") MultipartFile file,
                                                      @RequestParam(value = "title", required = false) String title) {
        Contract c = service.create(title, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Dto.ContractSummary.of(c));
    }

    /** List all contracts (metadata only). */
    @GetMapping
    public List<Dto.ContractSummary> list() {
        return service.list().stream().map(Dto.ContractSummary::of).toList();
    }

    /** 3. Fetch a contract with its parsed text. */
    @GetMapping("/{id}")
    public Dto.ContractDetail get(@PathVariable Long id) {
        return Dto.ContractDetail.of(service.get(id));
    }

    /** 2. Update status: DRAFT -> REVIEW -> APPROVED. */
    @PatchMapping("/{id}/status")
    public Dto.ContractSummary updateStatus(@PathVariable Long id, @Valid @RequestBody Dto.StatusRequest req) {
        ContractStatus target;
        try {
            target = ContractStatus.valueOf(req.status().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be one of DRAFT, REVIEW, APPROVED");
        }
        return Dto.ContractSummary.of(service.updateStatus(id, target));
    }

    /** 5. Ask a question about the contract. */
    @PostMapping("/{id}/ask")
    public QaService.Answer ask(@PathVariable Long id, @Valid @RequestBody Dto.QuestionRequest req) {
        return service.ask(id, req.question());
    }

    /** Download the original stored file. */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Contract c = service.get(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(c.getFileName()).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(service.original(c));
    }
}
