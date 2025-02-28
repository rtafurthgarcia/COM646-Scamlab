package model.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "vote")
public class Vote {
    @EmbeddedId
    private VoteId voteId;

    public VoteId getVoteId() {
        return voteId;
    }

    public void setVoteId(VoteId voteId) {
        this.voteId = voteId;
    }

    @CreationTimestamp
    private LocalDateTime creation;

    public LocalDateTime getCreation() {
        return creation;
    }

    @Column(nullable = false)
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((voteId == null) ? 0 : voteId.hashCode());
        result = prime * result + ((creation == null) ? 0 : creation.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
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
        Vote other = (Vote) obj;
        if (voteId == null) {
            if (other.voteId != null)
                return false;
        } else if (!voteId.equals(other.voteId))
            return false;
        if (creation == null) {
            if (other.creation != null)
                return false;
        } else if (!creation.equals(other.creation))
            return false;
        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;
        return true;
    }
}
