package org.example.tas_backend.entities;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Getter
@Setter
@Audited(withModifiedFlag = true)
@Embeddable
public class Address {
    String line1;
    String line2;
    String city;
    String region;
    String postalCode;
    String country;
}
