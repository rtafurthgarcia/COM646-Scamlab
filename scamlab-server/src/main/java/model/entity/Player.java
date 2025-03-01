package model.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "players", indexes = { 
    @Index(name = "idx_player_secondary_id", columnList = "secondary_id"),
    @Index(name = "idx_player_ip_address", columnList = "ip_address"), 
})
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    @Column(name = "secondary_id", unique = true)
    private UUID secondaryId = UUID.randomUUID();

    public UUID getSecondaryId() {
        return secondaryId;
    }

    @Column(unique = true, name = "ip_address", nullable = false)
    private String ipAddress;

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @CreationTimestamp
    private LocalDateTime creation;

    public LocalDateTime getCreation() {
        return creation;
    }

    @Version
    private Long version;

    public Long getVersion() {
        return version;
    }

    @Column(name = "is_bot", nullable = false)
    private Boolean isBot;

    public Boolean getIsBot() {
        return isBot;
    }

    public void setIsBot(Boolean isBot) {
        this.isBot = isBot;
    }

    @Column(nullable = false)
    private String token = "";

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
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
        Player other = (Player) obj;
        if (ipAddress == null) {
            if (other.ipAddress != null)
                return false;
        } else if (!ipAddress.equals(other.ipAddress))
            return false;
        return true;
    }
}