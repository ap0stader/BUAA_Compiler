package pass.analyzer;

import IR.IRModule;
import IR.value.IRBasicBlock;
import IR.value.IRFunction;
import IR.value.instruction.BranchInst;
import IR.value.instruction.IRInstruction;
import pass.Pass;

import java.util.Iterator;
import java.util.Objects;

public class GenerateCFG implements Pass {
    private final IRModule irModule;
    private boolean finished = false;

    public GenerateCFG(IRModule irModule) {
        this.irModule = irModule;
    }

    @Override
    public void run() {
        if (finished) {
            throw new RuntimeException("When GenerateCFG.run(), the pass is finished");
        }
        for (IRFunction irFunction : irModule.functions()) {
            if (!irFunction.isLib()) {
                this.generateFunctionCFG(irFunction);
                this.removeUnreachableBasicBlock(irFunction);
            }
        }
        this.finished = true;
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

    // 消除不可达的基本块
    private void removeUnreachableBasicBlock(IRFunction irFunction) {
        Iterator<IRBasicBlock> iterator = irFunction.basicBlocks().iterator();
        while (iterator.hasNext()) {
            IRBasicBlock basicBlock = iterator.next();
            if (!Objects.equals(basicBlock, irFunction.basicBlocks().get(0)) &&
                    basicBlock.predecessors().isEmpty()) {
                while (basicBlock.instructions().tail() != null) {
                    basicBlock.instructions().tail().value().eliminate();
                }
                iterator.remove();
            }
        }
    }

    @Override
    public void restart() {
        throw new RuntimeException("GenerateCFG can not restart");
    }
}
