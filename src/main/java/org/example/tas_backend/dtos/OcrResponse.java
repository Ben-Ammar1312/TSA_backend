package org.example.tas_backend.dtos;

public record OcrResponse(
        String filename,
        String ocr_text,
        Integer lines_count
        // plus List<CourseDTO> courses if you want
) {}
