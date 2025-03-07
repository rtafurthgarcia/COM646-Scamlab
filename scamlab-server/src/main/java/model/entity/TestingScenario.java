package model.entity;

public enum TestingScenario {
    OneBotTwoHumans(2),
    ThreeHumans(3);

    public final Integer numberOfHumans;

    private TestingScenario(Integer numberOfHumans) {
        this.numberOfHumans = numberOfHumans;
    }
}
