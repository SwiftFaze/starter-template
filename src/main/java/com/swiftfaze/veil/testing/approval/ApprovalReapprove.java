package com.swiftfaze.veil.testing.approval;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ApprovalReapprove {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApprovalReapprove.class);

    private ApprovalReapprove() {
    }

    private static final Path APPROVED_DIR = Paths.get("src/test/resources/approved");

    public static void main(String[] args) {
        try {
            reapproveAll(APPROVED_DIR);
        } catch (IOException e) {
            LOGGER.error("Error during re-approval: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    public static int reapproveAll(Path approvedDir) throws IOException {
        if (!Files.exists(approvedDir)) {
            LOGGER.info("No approved fixtures directory found at: {}", approvedDir);
            return 0;
        }

        File[] files = approvedDir.toFile().listFiles();
        if (files == null) {
            LOGGER.info("Failed to list files in: {}", approvedDir);
            return 0;
        }

        List<File> receivedFiles = Arrays.stream(files)
            .filter(f -> f.getName().endsWith(".received.txt"))
            .toList();

        if (receivedFiles.isEmpty()) {
            LOGGER.info("No .received.txt files found in: {}", approvedDir);
            return 0;
        }

        for (File receivedFile : receivedFiles) {
            String baseName = receivedFile.getName().replaceAll("\\.received\\.txt$", "");
            Path approvedPath = approvedDir.resolve(baseName + ".approved.txt");

            String receivedContent = Files.readString(receivedFile.toPath(), StandardCharsets.UTF_8);
            Files.writeString(approvedPath, receivedContent, StandardCharsets.UTF_8);

            Files.delete(receivedFile.toPath());

            LOGGER.info("Re-approved: {}", baseName);
        }

        LOGGER.info("Done. {} fixture(s) re-approved.", receivedFiles.size());
        return receivedFiles.size();
    }
}
