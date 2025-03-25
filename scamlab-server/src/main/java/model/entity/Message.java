package model.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_message_participation_id", columnList = "role_id, conversation_id, player_id"),
    @Index(name = "idx_message_photo_id", columnList = "photo_id"),
    @Index(name = "idx_message_creation", columnList = "creation")
})
public class Message {
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

    // TEXT is not portable, but for now it works
    @Column(length = 1024, nullable = false, columnDefinition = "TEXT")
    private String message;

    public String getMessage() {
        return message;
    }

    public Message setMessage(String message) {
        this.message = message;

        return this;
    }

    @Column(name = "llm_token_count", nullable = false)
    private Integer llmTokenCount = 0;
    
    public Integer getLlmTokenCount() {
        return llmTokenCount;
    }

    public Message setLlmTokenCount(Integer llmTokenCount) {
        this.llmTokenCount = llmTokenCount;

        return this;
    }

    @Column(name = "llm_generation_time", nullable = false)
    private Integer llmGenerationTime = 0;

    public Integer getLlmGenerationTime() {
        return llmGenerationTime;
    }

    public Message setLlmGenerationTime(Integer llmGenerationTime) {
        this.llmGenerationTime = llmGenerationTime;

        return this;
    }

    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "role_id", referencedColumnName = "role_id", nullable = false),
        @JoinColumn(name = "conversation_id", referencedColumnName = "conversation_id", nullable = false),
        @JoinColumn(name = "player_id", referencedColumnName = "player_id", nullable = false)
    })
    private Participation participation;

    public Participation getParticipation() {
        return participation;
    }

    public Message setParticipation(Participation participation) {
        this.participation = participation;

        return this;
    }
    
    @ManyToOne
    @JoinColumn(name = "photo_id", referencedColumnName = "id")
    private Photo photo;

    public Photo getPhoto() {
        return photo;
    }

    public Message setPhoto(Photo photo) {
        this.photo = photo;

        return this;
    }

    @Version
    private Long version;

    public Long getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + ((participation == null) ? 0 : participation.hashCode());
        result = prime * result + ((photo == null) ? 0 : photo.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Message other = (Message) obj;
        if (message == null) {
            if (other.message != null)
                return false;
        } else if (!message.equals(other.message))
            return false;
        if (participation == null) {
            if (other.participation != null)
                return false;
        } else if (!participation.equals(other.participation))
            return false;
        if (photo == null) {
            if (other.photo != null)
                return false;
        } else if (!photo.equals(other.photo))
            return false;
        return true;
    }
}
