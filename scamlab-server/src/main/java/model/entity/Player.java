package model.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NaturalId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

@Entity
@Table(name = "players", indexes = { 
    @Index(name = "idx_player_ip_address", columnList = "ip_address")
})
public class Player {

    public enum SystemRole {
        PLAYER,
        ADMIN;
    }

    @Transient
    private SystemRole systemRole;    

    public SystemRole getSystemRole() {
        return systemRole;
    }

    public Player setSystemRole(SystemRole systemRole) {
        this.systemRole = systemRole;

        return this;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    @NaturalId
    @Column(name = "secondary_id")
    //@GeneratedValue(strategy = GenerationType.UUID)
    private UUID secondaryId = UUID.randomUUID();

    public UUID getSecondaryId() {
        return secondaryId;
    }

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    public String getIpAddress() {
        return ipAddress;
    }

    public Player setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;

        return this;
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

    public Player setIsBot(Boolean isBot) {
        this.isBot = isBot;

        return this;
    }

    @Transient
    private String token = "";

    public String getToken() {
        return token;
    }

    public Player setToken(String token) {
        this.token = token;

        return this;
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