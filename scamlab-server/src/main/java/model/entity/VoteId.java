package model.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Embeddable
public class VoteId implements Serializable {
    @ManyToOne
    @JoinColumn(name = "player_id", referencedColumnName = "id")
    private Player player;

    @ManyToOne
    @JoinColumn(name = "conversation_id", referencedColumnName = "id")
    private Conversation conversation;

    @Column(name = "round_no")
    private Integer RoundNo;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((player == null) ? 0 : player.hashCode());
        result = prime * result + ((conversation == null) ? 0 : conversation.hashCode());
        result = prime * result + ((RoundNo == null) ? 0 : RoundNo.hashCode());
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
        VoteId other = (VoteId) obj;
        if (player == null) {
            if (other.player != null)
                return false;
        } else if (!player.equals(other.player))
            return false;
        if (conversation == null) {
            if (other.conversation != null)
                return false;
        } else if (!conversation.equals(other.conversation))
            return false;
        if (RoundNo == null) {
            if (other.RoundNo != null)
                return false;
        } else if (!RoundNo.equals(other.RoundNo))
            return false;
        return true;
    }

    public Player getPlayer() {
        return player;
    }

    public VoteId setPlayer(Player player) {
        this.player = player;

        return this;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public VoteId setConversation(Conversation conversation) {
        this.conversation = conversation;

        return this;
    }

    public Integer getRoundNo() {
        return RoundNo;
    }

    public VoteId setRoundNo(Integer roundNo) {
        RoundNo = roundNo;

        return this;
    }
}