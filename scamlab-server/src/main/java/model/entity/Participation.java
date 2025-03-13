package model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "participations", indexes = {
    @Index(name = "idx_participation_username", columnList = "username")
})
public class Participation {
    @EmbeddedId
    private ParticipationId participationId;

    public Participation setParticipationId(ParticipationId participationId) {
        this.participationId = participationId;

        return this;
    }

    public ParticipationId getParticipationId() {
        return participationId;
    }

    @Column(nullable = false, name = "username")
    private String userName;

    public String getUserName() {
        return userName;
    }

    public Participation setUserName(String name) {
        this.userName = name;

        return this;
    }
}
