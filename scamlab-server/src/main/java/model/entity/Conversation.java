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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

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
    @NotNull
    @Column(name = "testing_scenario")
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

    @NotNull
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

    @OneToMany(mappedBy = "stateTransitionId.conversation", cascade=CascadeType.ALL) // ✅ Correct path
    @OrderBy("stateTransitionId.creation")
    private List<StateTransition> states = new ArrayList<>();

    public List<StateTransition> getStates() {
        return states;
    }

    @Version
    private Long version;

    public Long getVersion() {
        return version;
    }
}
