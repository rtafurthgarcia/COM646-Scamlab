package model.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NaturalId;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "players", indexes = { 
    @Index(name = "idx_player_ip_address", columnList = "ip_address")
})
public class Player {
    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = false)
    private SystemRole systemRole = SystemRole.USER;    

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

    @Lob 
    @Basic(fetch = FetchType.EAGER)
    private String token;

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
        result = prime * result + ((systemRole == null) ? 0 : systemRole.hashCode());
        result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
        result = prime * result + ((isBot == null) ? 0 : isBot.hashCode());
        result = prime * result + ((token == null) ? 0 : token.hashCode());
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
        if (systemRole != other.systemRole)
            return false;
        if (ipAddress == null) {
            if (other.ipAddress != null)
                return false;
        } else if (!ipAddress.equals(other.ipAddress))
            return false;
        if (isBot == null) {
            if (other.isBot != null)
                return false;
        } else if (!isBot.equals(other.isBot))
            return false;
        if (token == null) {
            if (other.token != null)
                return false;
        } else if (!token.equals(other.token))
            return false;
        return true;
    }
}