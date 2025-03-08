package helper;

public class DefaultKeyValues {
    public enum StateValue {
        WAITING(1),
        READY(2),
        RUNNING(3),
        VOTING(4),
        FINISHED_UNMASKED(5),
        FINISHED_MASKED(6),
        CANCELLED(7);

        public final Long value;

        private StateValue(Integer value) {
            this.value = Integer.toUnsignedLong(value);
        }
    }

    public enum RoleValue {
        SCAMBAITER(1),
        SCAMMER(2);

        public final Long value;

        private RoleValue(Integer value) {
            this.value = Integer.toUnsignedLong(value);
        }
    }
}
