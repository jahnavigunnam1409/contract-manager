package com.example.contracts.service;

import com.example.contracts.model.Contract;
import com.example.contracts.model.ContractStatus;
import com.example.contracts.repo.ContractRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@Service
public class ContractService {

    private final ContractRepository repo;
    private final StorageService storage;
    private final DocumentParser parser;
    private final QaService qa;

    public ContractService(ContractRepository repo, StorageService storage, DocumentParser parser, QaService qa) {
        this.repo = repo;
        this.storage = storage;
        this.parser = parser;
        this.qa = qa;
    }

    @Transactional
    public Contract create(String title, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A file is required");
        }
        String original = file.getOriginalFilename() == null ? "contract" : file.getOriginalFilename();
        String type = DocumentParser.detectType(original);
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF, DOC and DOCX files are supported");
        }
        try {
            byte[] bytes = file.getBytes();
            String text = parser.extractText(bytes, type);

            Contract c = new Contract();
            c.setTitle(title == null || title.isBlank() ? stripExtension(original) : title.trim());
            c.setFileName(original);
            c.setFileType(type);
            c.setStoragePath(storage.save(bytes, type));
            c.setExtractedText(text);
            c.setWordCount(text.isBlank() ? 0 : text.trim().split("\\s+").length);
            c.setStatus(ContractStatus.DRAFT);
            return repo.save(c);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not read the document. Is it corrupted or password protected?");
        }
    }

    @Transactional(readOnly = true)
    public List<Contract> list() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public Contract get(Long id) {
        return repo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract " + id + " not found"));
    }

    @Transactional
    public Contract updateStatus(Long id, ContractStatus newStatus) {
        Contract c = get(id);
        if (c.getStatus() == newStatus) return c;
        if (!c.getStatus().canMoveTo(newStatus)) {
            ContractStatus next = c.getStatus().next();
            String hint = next == null ? "It is already final." : "Next allowed status: " + next + ".";
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invalid transition " + c.getStatus() + " -> " + newStatus + ". " + hint);
        }
        c.setStatus(newStatus);
        return repo.save(c);
    }

    @Transactional(readOnly = true)
    public QaService.Answer ask(Long id, String question) {
        return qa.ask(get(id).getExtractedText(), question);
    }

    public byte[] original(Contract c) {
        try {
            return storage.load(c.getStoragePath());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored file is missing");
        }
    }

    private String stripExtension(String name) {
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(0, i) : name;
    }
}
