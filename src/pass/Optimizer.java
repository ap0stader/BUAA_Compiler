package pass;

import IR.IRModule;
import pass.refactor.RemoveInstructionAfterTerminator;

public class Optimizer {
    private final IRModule irModule;

    public Optimizer(IRModule irModule) {
        this.irModule = irModule;
    }

    public void optimize() {
        new RemoveInstructionAfterTerminator(irModule).run();

    }
}
