package model.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "votes", indexes = {
    @Index(name = "idx_vote_creation", columnList = "creation")
})
public class Vote implements Comparable<Vote> {
    @EmbeddedId
    private VoteId voteId;

    public VoteId getVoteId() {
        return voteId;
    }

    public Vote setVoteId(VoteId voteId) {
        this.voteId = voteId;

        return this;
    }

    @ManyToOne
    @JoinColumn(name = "player_voted_against_id", referencedColumnName = "id", nullable = true)
    private Player playerVotedAgainst;

    public Player getPlayerVotedAgainst() {
        return playerVotedAgainst;
    }

    public Vote setPlayerVotedAgainst(Player playerVotedAgainst) {
        this.playerVotedAgainst = playerVotedAgainst;

        return this;
    }

    @CreationTimestamp
    private LocalDateTime creation;

    public LocalDateTime getCreation() {
        return creation;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((voteId == null) ? 0 : voteId.hashCode());
        result = prime * result + ((creation == null) ? 0 : creation.hashCode());
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
        return true;
    }

    @Override
    public int compareTo(Vote o) {
        return voteId.getRoundNo() - o.voteId.getRoundNo();
    }
}
