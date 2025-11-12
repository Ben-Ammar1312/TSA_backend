package org.example.tas_backend.dtos;

public record AddressDTO(
        String line1,
        String line2,
        String city,
        String stateOrProvince,
        String postalCode,
        String country
) {}