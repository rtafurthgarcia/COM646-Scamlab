package model.entity;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Embeddable
public class StrategyByRoleId implements Serializable {
    @ManyToOne
    @JoinColumn(name = "strategy_id", referencedColumnName = "id", nullable = false)
    private Strategy strategy;

    @ManyToOne
    @JoinColumn(name = "role_id", referencedColumnName = "id", nullable = false)
    private Role role;

    public Strategy getStrategy() {
        return strategy;
    }

    public StrategyByRoleId setStrategy(Strategy strategy) {
        this.strategy = strategy;

        return this;
    }

    public Role getRole() {
        return role;
    }

    public StrategyByRoleId setRole(Role role) {
        this.role = role;

        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((strategy == null) ? 0 : strategy.hashCode());
        result = prime * result + ((role == null) ? 0 : role.hashCode());
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
        StrategyByRoleId other = (StrategyByRoleId) obj;
        if (strategy == null) {
            if (other.strategy != null)
                return false;
        } else if (!strategy.equals(other.strategy))
            return false;
        if (role == null) {
            if (other.role != null)
                return false;
        } else if (!role.equals(other.role))
            return false;
        return true;
    }
}
