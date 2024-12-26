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
        // 必须消除每个基本块第一条终结指令之后的指令，这样才是正确的LLVM
        this.passes.add(new RemoveInstructionAfterTerminator(irModule));
        // 后端的跳转依赖CFG信息工作
        this.passes.add(new GenerateCFG(irModule));
        // 线性寄存器分配依赖于消除不可到达的基本块
        if (Config.enableBackendOptimization || Config.enableMiddleOptimization) {
            this.passes.add(new RemoveUnreachableBasicBlock(irModule));
        }
        if (Config.enableMiddleOptimization) {
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
