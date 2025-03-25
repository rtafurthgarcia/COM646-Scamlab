package model.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NaturalId;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_conversation_testing_scenario", columnList = "testing_scenario"),
    @Index(name = "idx_conversation_testing_day", columnList = "testing_day"),
    @Index(name = "idx_conversation_strategy_id", columnList = "strategy_id"),
    @Index(name = "idx_conversation_state_id", columnList = "state_id")
})
public class Conversation {
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
    @Enumerated(EnumType.STRING)
    @Column(name = "testing_scenario", nullable = false)
    private TestingScenario testingScenario;

    public TestingScenario getTestingScenario() {
        return testingScenario;
    }
    public Conversation setTestingScenario(TestingScenario testingScenario) {
        this.testingScenario = testingScenario;

        return this;
    }
    @CreationTimestamp
    @Column(name = "testing_day")
    private LocalDate testingDay;

    @Column(nullable = false)
    private boolean invalidated = false;

    public boolean isInvalidated() {
        return invalidated;
    }
    public Conversation setInvalidated(boolean invalidatedOrNot) {
        this.invalidated = invalidatedOrNot;

        return this;
    }

    @Column(name = "start_of_conversation")
    private LocalTime start;
    public LocalTime getStart() {
        return start;
    }
    public Conversation setStart(LocalTime start) {
        this.start = start;

        return this;
    }
    
    @Column(name = "end_of_conversation")
    private LocalTime end;

    public LocalTime getEnd() {
        return end;
    }
    public Conversation setEnd(LocalTime end) {
        this.end = end;

        return this;
    }

    @OneToMany(mappedBy = "stateTransitionId.conversation") 
    @OrderBy("stateTransitionId.creation")
    private List<StateTransition> states = new ArrayList<>();

    public List<StateTransition> getStates() {
        return states;
    }

    @ManyToOne
    @JoinColumn(name = "state_id", referencedColumnName = "id", nullable = false)
    private State currentState; 

    public State getCurrentState() {
        return currentState;
    }

    public Conversation setCurrentState(State newState, TransitionReason reason) {
        this.currentState = newState;

        this.getStates().add(
            new StateTransition()
                .setStateTransitionId(
                    new StateTransitionId().setConversation(this).setState(newState)
                )
                .setReason(reason)
        );

        return this;
    }

    public Conversation setCurrentState(State newState) {
        return setCurrentState(newState, null);
    }

    @OneToMany(orphanRemoval = false)
    @JoinColumn(name = "conversation_id") 
    @OrderBy("creation")
    private List<Message> messages = new ArrayList<>();

    public List<Message> getMessages() {
        return messages;
    }

    @OneToMany(mappedBy = "participationId.conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participation> participants = new ArrayList<>();

    public List<Participation> getParticipants() {
        return participants;
    }

    @OneToMany(mappedBy = "voteId.conversation")
    private List<Vote> votes = new ArrayList<>();

    public List<Vote> getVotes() {
        return votes;
    }

    @ManyToOne
    @JoinColumn(name = "strategy_id", referencedColumnName = "id")
    private Strategy strategy;

    public Strategy getStrategy() {
        return strategy;
    }
    public Conversation setStrategy(Strategy strategy) {
        this.strategy = strategy;

        return this;
    }

    @Column(nullable = true)
    private Boolean botHasBeenUnmasked = null;

    public Boolean getBotHasBeenUnmasked() {
        return botHasBeenUnmasked;
    }
    public Conversation setBotHasBeenUnmasked(Boolean unmaskedOrNot) {
        this.botHasBeenUnmasked = unmaskedOrNot;

        return this;
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
        result = prime * result + ((start == null) ? 0 : start.hashCode());
        result = prime * result + ((end == null) ? 0 : end.hashCode());
        result = prime * result + ((participants == null) ? 0 : participants.hashCode());
        result = prime * result + ((strategy == null) ? 0 : strategy.hashCode());
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
        if (participants == null) {
            if (other.participants != null)
                return false;
        } else if (!participants.equals(other.participants))
            return false;
        if (strategy == null) {
            if (other.strategy != null)
                return false;
        } else if (!strategy.equals(other.strategy))
            return false;
        return true;
    }
}
