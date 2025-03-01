package model.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.jspecify.annotations.NonNull;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "photos", indexes = @Index(name = "idx_photo_name", columnList = "name"))
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    @CreationTimestamp
    private LocalDateTime creation;

    public LocalDateTime getCreation() {
        return creation;
    }

    @Column(unique = true, nullable = false)
    private String name;

    public String getName() {
        return name;
    }

    @Column(unique = true, nullable = false)
    private String description;

    public String getDescription() {
        return description;
    }

    @Column(name = "resource_path", unique = true, nullable = false)
    private String resourcePath;

    public String getResourcePath() {
        return resourcePath;
    }

    @Version
    private Long version;

    public Long getVersion() {
        return version;
    }
}
