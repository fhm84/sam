package de.halbmann.sam.business.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class FingerprintEntity {

    @NotBlank
    @Column(name = "fingerprint", length = 64, nullable = false)
    String hashValue;

    @Column(name = "fingerprint_version", nullable = false)
    int version;
}
