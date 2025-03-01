package model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "strategies_by_role")
public class StrategyByRole {
    @EmbeddedId 
    private StrategyByRoleId strategyByRoleId;

    public StrategyByRoleId getStrategyByRoleId() {
        return strategyByRoleId;
    }

    @Lob 
    @Column(nullable = false, length = 1024)
    private String script;

    @Lob 
    @Column(nullable = false, length = 1024)
    private String example;

    @Lob 
    @Column(name = "evasion_example", nullable = false, length = 1024)
    private String evasionExample;

    public String getScript() {
        return script;
    }

    public String getExample() {
        return example;
    }

    public String getEvasionExample() {
        return evasionExample;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((script == null) ? 0 : script.hashCode());
        result = prime * result + ((example == null) ? 0 : example.hashCode());
        result = prime * result + ((evasionExample == null) ? 0 : evasionExample.hashCode());
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
        StrategyByRole other = (StrategyByRole) obj;
        if (script == null) {
            if (other.script != null)
                return false;
        } else if (!script.equals(other.script))
            return false;
        if (example == null) {
            if (other.example != null)
                return false;
        } else if (!example.equals(other.example))
            return false;
        if (evasionExample == null) {
            if (other.evasionExample != null)
                return false;
        } else if (!evasionExample.equals(other.evasionExample))
            return false;
        return true;
    }
}
