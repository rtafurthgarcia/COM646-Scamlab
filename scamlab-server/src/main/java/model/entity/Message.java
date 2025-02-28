package model.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.jspecify.annotations.NonNull;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "message")
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

    @NonNull
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @NonNull
    @Column(name = "llm_token_count")
    private Integer llmTokenCount = 0;
    
    public Integer getLlmTokenCount() {
        return llmTokenCount;
    }

    public void setLlmTokenCount(Integer llmTokenCount) {
        this.llmTokenCount = llmTokenCount;
    }

    @NonNull
    @Column(name = "llm_generation_time")
    private Integer llmGenerationTime = 0;

    public Integer getLlmGenerationTime() {
        return llmGenerationTime;
    }

    public void setLlmGenerationTime(Integer llmGenerationTime) {
        this.llmGenerationTime = llmGenerationTime;
    }

    @ManyToOne
    @JoinColumn(name = "conversation_id", referencedColumnName = "id")
    private Conversation conversation;

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    @ManyToOne
    @JoinColumn(name = "photo_id", referencedColumnName = "id")
    private Photo photo;

    public Photo getPhoto() {
        return photo;
    }

    public void setPhoto(Photo photo) {
        this.photo = photo;
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
        result = prime * result + ((llmTokenCount == null) ? 0 : llmTokenCount.hashCode());
        result = prime * result + ((llmGenerationTime == null) ? 0 : llmGenerationTime.hashCode());
        result = prime * result + ((conversation == null) ? 0 : conversation.hashCode());
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
        if (llmTokenCount == null) {
            if (other.llmTokenCount != null)
                return false;
        } else if (!llmTokenCount.equals(other.llmTokenCount))
            return false;
        if (llmGenerationTime == null) {
            if (other.llmGenerationTime != null)
                return false;
        } else if (!llmGenerationTime.equals(other.llmGenerationTime))
            return false;
        if (conversation == null) {
            if (other.conversation != null)
                return false;
        } else if (!conversation.equals(other.conversation))
            return false;
        if (photo == null) {
            if (other.photo != null)
                return false;
        } else if (!photo.equals(other.photo))
            return false;
        return true;
    }

   
}
