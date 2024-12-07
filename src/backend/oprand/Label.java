package backend.oprand;

public class Label extends TargetOperand {
    private final String name;

    public Label(String name) {
        this.name = name;
    }

    public String mipsStr() {
        return this.name;
    }
}
