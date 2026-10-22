package com.example.contracts.web;

import com.example.contracts.model.Contract;
import com.example.contracts.model.ContractStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class Dto {
    private Dto() {}

    public record StatusRequest(@NotBlank String status) {}
    public record QuestionRequest(@NotBlank String question) {}

    /** Metadata view (no full text) used in lists and after create/update. */
    public record ContractSummary(Long id, String title, String fileName, String fileType,
                                  ContractStatus status, int wordCount, Instant createdAt, Instant updatedAt) {
        public static ContractSummary of(Contract c) {
            return new ContractSummary(c.getId(), c.getTitle(), c.getFileName(), c.getFileType(),
                    c.getStatus(), c.getWordCount(), c.getCreatedAt(), c.getUpdatedAt());
        }
    }

    /** Full view including parsed text. */
    public record ContractDetail(Long id, String title, String fileName, String fileType,
                                 ContractStatus status, int wordCount, Instant createdAt, Instant updatedAt,
                                 String extractedText) {
        public static ContractDetail of(Contract c) {
            return new ContractDetail(c.getId(), c.getTitle(), c.getFileName(), c.getFileType(),
                    c.getStatus(), c.getWordCount(), c.getCreatedAt(), c.getUpdatedAt(), c.getExtractedText());
        }
    }
}
