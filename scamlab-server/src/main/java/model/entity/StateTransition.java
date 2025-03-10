package model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "state_transitions", indexes = {
    @Index(name = "idx_state_transition_creation", columnList = "creation"),
    @Index(name = "idx_state_transition_reason", columnList = "reason"),
})
public class StateTransition {
    @EmbeddedId
    private StateTransitionId stateTransitionId;

    public StateTransitionId getStateTransitionId() {
        return stateTransitionId;
    }

    public StateTransition setStateTransitionId(StateTransitionId stateTransitionId) {
        this.stateTransitionId = stateTransitionId;

        return this;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private TransitionReason reason;

    public TransitionReason getReason() {
        return reason;
    }

    public StateTransition setReason(TransitionReason reason) {
        this.reason = reason;

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
        result = prime * result + ((stateTransitionId == null) ? 0 : stateTransitionId.hashCode());
        result = prime * result + ((reason == null) ? 0 : reason.hashCode());
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
        StateTransition other = (StateTransition) obj;
        if (stateTransitionId == null) {
            if (other.stateTransitionId != null)
                return false;
        } else if (!stateTransitionId.equals(other.stateTransitionId))
            return false;
        if (reason == null) {
            if (other.reason != null)
                return false;
        } else if (!reason.equals(other.reason))
            return false;
        return true;
    }
}
