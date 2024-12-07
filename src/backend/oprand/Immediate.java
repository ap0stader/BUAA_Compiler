package backend.oprand;

public class Immediate extends TargetOperand {
    private final Integer value;

    public Immediate(Integer value) {
        this.value = value;
    }

    @Override
    public String mipsStr() {
        return "0x" + Integer.toHexString(value).toUpperCase();
    }
}
