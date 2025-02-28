package model.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "conversation", indexes = @Index(name = "idx_conversation_multiple", columnList = "secondary_id, testing_scenario, invalidated, testing_day"))
public class Conversation {
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
    @Enumerated(EnumType.STRING)
    @Column(name = "testing_scenario", nullable = false)
    private TestingScenario testingScenario;

    public TestingScenario getTestingScenario() {
        return testingScenario;
    }
    public void setTestingScenario(TestingScenario testingScenario) {
        this.testingScenario = testingScenario;
    }
    @CreationTimestamp
    @Column(name = "testing_day")
    private LocalDate testingDay;

    @Column(nullable = false)
    private boolean invalidated = false;

    public boolean isInvalidated() {
        return invalidated;
    }
    public void setInvalidated(boolean invalidated) {
        this.invalidated = invalidated;
    }

    @Column(name = "start_of_conversation")
    private LocalTime start;
    public LocalTime getStart() {
        return start;
    }
    public void setStart(LocalTime start) {
        this.start = start;
    }
    
    @Column(name = "end_of_conversation")
    private LocalTime end;

    public LocalTime getEnd() {
        return end;
    }
    public void setEnd(LocalTime end) {
        this.end = end;
    }

    @OneToMany(mappedBy = "stateTransitionId.conversation", cascade=CascadeType.ALL) 
    @OrderBy("stateTransitionId.creation")
    private List<StateTransition> states = new ArrayList<>();

    public List<StateTransition> getStates() {
        return states;
    }

    @OneToMany(mappedBy = "conversation", cascade=CascadeType.ALL) 
    @OrderBy("creation")
    private List<Message> messages = new ArrayList<>();

    public List<Message> getMessages() {
        return messages;
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
        result = prime * result + ((testingScenario == null) ? 0 : testingScenario.hashCode());
        result = prime * result + ((testingDay == null) ? 0 : testingDay.hashCode());
        result = prime * result + (invalidated ? 1231 : 1237);
        result = prime * result + ((start == null) ? 0 : start.hashCode());
        result = prime * result + ((end == null) ? 0 : end.hashCode());
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
        Conversation other = (Conversation) obj;
        if (testingScenario != other.testingScenario)
            return false;
        if (testingDay == null) {
            if (other.testingDay != null)
                return false;
        } else if (!testingDay.equals(other.testingDay))
            return false;
        if (invalidated != other.invalidated)
            return false;
        if (start == null) {
            if (other.start != null)
                return false;
        } else if (!start.equals(other.start))
            return false;
        if (end == null) {
            if (other.end != null)
                return false;
        } else if (!end.equals(other.end))
            return false;
        return true;
    }
}
