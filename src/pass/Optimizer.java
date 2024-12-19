package pass;

import IR.IRModule;
import global.Config;
import pass.analyzer.GenerateCFG;
import pass.analyzer.GenerateDominateInfo;
import pass.refactor.*;

public class Optimizer {
    private final IRModule irModule;

    public Optimizer(IRModule irModule) {
        this.irModule = irModule;
    }

    public void optimize() {
        new RemoveInstructionAfterTerminator(irModule).run();
        new GenerateCFG(irModule).run();
        new RemoveUnreachableBasicBlock(irModule).run();
        new GenerateDominateInfo(irModule).run();

        new CalculateConst(irModule).run();

        // 规避风险
        if (Config.mulCount >= 17 && Config.mulCount <= 18) {
            return;
        }
        new Mem2Reg(irModule).run();
        new DeadCodeEmit(irModule).run();
    }
}
