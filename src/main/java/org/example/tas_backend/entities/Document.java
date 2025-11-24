package org.example.tas_backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.tas_backend.enums.DocumentType;
import org.hibernate.envers.Audited;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Getter
@Setter
@Audited(withModifiedFlag = true)
@Table(indexes = {@Index(name="ix_doc_app", columnList="application_id"),
        @Index(name="ix_doc_type", columnList="type")})
public class Document {
    @Id
    @GeneratedValue(strategy=IDENTITY) Long id;
    @ManyToOne(fetch=LAZY) Application application;

    @Enumerated(EnumType.STRING)
    DocumentType type;
    String filename;
    String storageKey; // S3/GCS path
    String mimeType;
    Long sizeBytes;

    // OCR output id or short text preview
    String ocrJobId;
    @Lob
    @Column(columnDefinition = "text")
    String rawText;

    @Embedded
    private Audit audit = new Audit();

    @PrePersist
    void prePersist() {
        if (audit == null) audit = new Audit();
    }
}
