package model.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Embeddable
public class StateTransitionId implements Serializable {
    @ManyToOne
    @JoinColumn(name = "state_id", referencedColumnName = "id") // Replace with your FK column
    private State state;

    @ManyToOne
    @JoinColumn(name = "conversation_id", referencedColumnName = "id")
    private Conversation conversation;
 
    private LocalDateTime creation = LocalDateTime.now();

    public State getState() {
        return state;
    }

    public StateTransitionId setState(State state) {
        this.state = state;

        return this;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public StateTransitionId setConversation(Conversation conversation) {
        this.conversation = conversation;

        return this;
    }

    public LocalDateTime getCreation() {
        return creation;
    }

    public StateTransitionId setCreation(LocalDateTime creation) {
        this.creation = creation;

        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        result = prime * result + ((conversation == null) ? 0 : conversation.hashCode());
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
        StateTransitionId other = (StateTransitionId) obj;
        if (state == null) {
            if (other.state != null)
                return false;
        } else if (!state.equals(other.state))
            return false;
        if (conversation == null) {
            if (other.conversation != null)
                return false;
        } else if (!conversation.equals(other.conversation))
            return false;
        if (creation == null) {
            if (other.creation != null)
                return false;
        } else if (!creation.equals(other.creation))
            return false;
        return true;
    }
}
