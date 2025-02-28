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
@Table(name = "photo", indexes = @Index(name = "idx_photo_multiple", columnList = "name, resource_path"))
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

    @NonNull
    @Column(unique = true)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NonNull
    @Column(name = "resource_path", unique = true)
    private String resourcePath;

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String ResourcePath) {
        this.resourcePath = ResourcePath;
    }

    @Version
    private Long version;

    public Long getVersion() {
        return version;
    }
}
