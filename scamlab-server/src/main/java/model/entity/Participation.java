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

    public void setParticipationId(ParticipationId participationId) {
        this.participationId = participationId;
    }

    public ParticipationId getParticipationId() {
        return participationId;
    }

    @Column(nullable = false, name = "username")
    private String userName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String name) {
        this.userName = name;
    }

    @Column(name = "rate_of_confidence", nullable = true)
    private Integer rateOfConfidence;

    public Integer getRateOfConfidence() {
        return rateOfConfidence;
    }

    public void setRateOfConfidence(Integer rateOfConfidence) {
        this.rateOfConfidence = rateOfConfidence;
    }

    @Column(name = "rate_of_experience", nullable = true)
    private Integer rateOfExperience;

    public Integer getRateOfExperience() {
        return rateOfExperience;
    }

    public void setRateOfExperience(Integer rateOfExperience) {
        this.rateOfExperience = rateOfExperience;
    }

    @Column(name = "giveaway", nullable = true)
    private String giveaway;

    public String getGiveaway() {
        return giveaway;
    }

    public void setGiveaway(String giveaway) {
        this.giveaway = giveaway;
    }
}
