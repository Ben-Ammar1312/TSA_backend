package org.example.tas_backend.dtos;

public record OcrResponse(
        String filename,
        String ocr_text,
        Integer lines_count,
        java.util.List<String> courses
) {}
