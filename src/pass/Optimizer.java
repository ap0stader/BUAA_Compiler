package pass;

import IR.IRModule;
import global.Config;
import pass.analyzer.GenerateCFG;
import pass.analyzer.GenerateDominateInfo;
import pass.refactor.*;

import java.util.ArrayList;

public class Optimizer {
    private final ArrayList<Pass> passes;

    public Optimizer(IRModule irModule) {
        this.passes = new ArrayList<>();
        this.passes.add(new RemoveInstructionAfterTerminator(irModule));
        this.passes.add(new GenerateCFG(irModule));
        if (Config.enableMiddleOptimization) {
            this.passes.add(new RemoveUnreachableBasicBlock(irModule));
            this.passes.add(new GenerateDominateInfo(irModule));
            this.passes.add(new Mem2Reg(irModule));
            this.passes.add(new CalculateConst(irModule));
            this.passes.add(new DeadCodeEmit(irModule));
        }
    }

    public void optimize() {
        for (Pass pass : passes) {
            pass.run();
        }
    }
}
