package backend.target;

import backend.oprand.Label;

public class TargetBasicBlock {
    private final TargetFunction parent;
    private final Label label;

    public TargetBasicBlock(TargetFunction parent, int order) {
        this.parent = parent;
        this.label = new Label(parent.label().name() + "." + order);
    }

    public String mipsStr() {
        return this.label.mipsStr() + ":\n";
    }
}
