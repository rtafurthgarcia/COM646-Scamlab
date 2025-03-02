package model.entity;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Embeddable
public class ParticipationId implements Serializable {
    @ManyToOne
    @JoinColumn(name = "role_id", referencedColumnName = "id") // Replace with your FK column
    private Role role;

    @ManyToOne
    @JoinColumn(name = "conversation_id", referencedColumnName = "id")
    private Conversation conversation;

    @ManyToOne
    @JoinColumn(name = "player_id", referencedColumnName = "id")
    private Player player;

    public Role getRole() {
        return role;
    }

    public ParticipationId setRole(Role role) {
        this.role = role;

        return this;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public ParticipationId setConversation(Conversation conversation) {
        this.conversation = conversation;

        return this;
    }

    public Player getPlayer() {
        return player;
    }

    public ParticipationId setPlayer(Player player) {
        this.player = player;

        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((role == null) ? 0 : role.hashCode());
        result = prime * result + ((conversation == null) ? 0 : conversation.hashCode());
        result = prime * result + ((player == null) ? 0 : player.hashCode());
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
        ParticipationId other = (ParticipationId) obj;
        if (role == null) {
            if (other.role != null)
                return false;
        } else if (!role.equals(other.role))
            return false;
        if (conversation == null) {
            if (other.conversation != null)
                return false;
        } else if (!conversation.equals(other.conversation))
            return false;
        if (player == null) {
            if (other.player != null)
                return false;
        } else if (!player.equals(other.player))
            return false;
        return true;
    }
}
