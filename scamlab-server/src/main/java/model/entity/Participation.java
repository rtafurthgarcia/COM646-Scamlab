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

    @Column(name = "rate_of_confidence", nullable = true)
    private Integer rateOfConfidence;

    public Integer getRateOfConfidence() {
        return rateOfConfidence;
    }

    public Participation setRateOfConfidence(Integer rateOfConfidence) {
        this.rateOfConfidence = rateOfConfidence;

        return this;
    }

    @Column(name = "rate_of_experience", nullable = true)
    private Integer rateOfExperience;

    public Integer getRateOfExperience() {
        return rateOfExperience;
    }

    public Participation setRateOfExperience(Integer rateOfExperience) {
        this.rateOfExperience = rateOfExperience;

        return this;
    }

    @Column(name = "giveaway", nullable = true)
    private String giveaway;

    public String getGiveaway() {
        return giveaway;
    }

    public Participation setGiveaway(String giveaway) {
        this.giveaway = giveaway;

        return this;
    }
}
