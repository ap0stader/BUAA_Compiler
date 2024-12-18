package pass;

import IR.IRModule;
import pass.analyzer.GenerateCFG;
import pass.analyzer.GenerateDominateInfo;
import pass.refactor.RemoveInstructionAfterTerminator;

public class Optimizer {
    private final IRModule irModule;

    public Optimizer(IRModule irModule) {
        this.irModule = irModule;
    }

    public void optimize() {
        new RemoveInstructionAfterTerminator(irModule).run();
        new GenerateCFG(irModule).run();
        new GenerateDominateInfo(irModule).run();
    }
}
