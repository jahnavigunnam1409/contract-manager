package com.example.contracts.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class StorageService {

    private final Path root;

    public StorageService(@Value("${app.storage.dir}") String dir) throws IOException {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    /** Stores bytes under a random name (never trusts the client file name) and returns the path. */
    public String save(byte[] data, String fileType) throws IOException {
        Path target = root.resolve(UUID.randomUUID() + "." + fileType.toLowerCase());
        Files.write(target, data);
        return target.toString();
    }

    public byte[] load(String storagePath) throws IOException {
        return Files.readAllBytes(Path.of(storagePath));
    }
}
