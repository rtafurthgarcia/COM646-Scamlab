package model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "state_transition")
public class StateTransition {
    @EmbeddedId
    private StateTransitionId stateTransitionId;

    private String reason = "";

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Version
    private Long version;

    public Long getVersion() {
        return version;
    }
}
