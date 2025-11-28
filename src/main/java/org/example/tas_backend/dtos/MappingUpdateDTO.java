package org.example.tas_backend.dtos;

import lombok.Data;

@Data
public class MappingUpdateDTO {
    private String targetCode;
    private Float confidence;
}
