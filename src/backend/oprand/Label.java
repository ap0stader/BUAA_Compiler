package backend.oprand;

public record Label(String name) implements TargetOperand {
    @Override
    public String mipsStr() {
        return this.name;
    }
}
