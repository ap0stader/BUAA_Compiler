package pass.analyzer;

import IR.IRModule;
import IR.value.IRBasicBlock;
import IR.value.IRFunction;
import IR.value.instruction.BranchInst;
import IR.value.instruction.IRInstruction;
import pass.Pass;

public class GenerateCFG implements Pass {
    private final IRModule irModule;

    public GenerateCFG(IRModule irModule) {
        this.irModule = irModule;
    }

    @Override
    public void run() {
        for (IRFunction irFunction : irModule.functions()) {
            if (!irFunction.isLib()) {
                this.generateFunctionCFG(irFunction);
            }
        }
    }

    private void generateFunctionCFG(IRFunction irFunction) {
        for (IRBasicBlock basicBlock : irFunction.basicBlocks()) {
            IRInstruction<?> tailInstruction = basicBlock.instructions().tail().value();
            if (tailInstruction instanceof BranchInst tailBranchInstruction) {
                if (tailBranchInstruction.isConditional()) {
                    basicBlock.successors().add(tailBranchInstruction.getTrueSuccessor());
                    basicBlock.successors().add(tailBranchInstruction.getFalseSuccessor());
                    tailBranchInstruction.getTrueSuccessor().predecessors().add(basicBlock);
                    tailBranchInstruction.getFalseSuccessor().predecessors().add(basicBlock);
                } else {
                    basicBlock.successors().add(tailBranchInstruction.getSuccessor());
                    tailBranchInstruction.getSuccessor().predecessors().add(basicBlock);
                }
            }
        }
    }
}
