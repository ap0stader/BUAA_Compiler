package pass.analyzer;

import IR.IRModule;
import IR.value.IRBasicBlock;
import IR.value.IRFunction;
import pass.Pass;

import java.util.HashSet;
import java.util.Objects;

public class GenerateDominateInfo implements Pass {
    private final IRModule irModule;

    public GenerateDominateInfo(IRModule irModule) {
        this.irModule = irModule;
    }

    @Override
    public void run() {
        for (IRFunction irFunction : irModule.functions()) {
            if (!irFunction.isLib()) {
                this.generateDominateInfo(irFunction);
            }
        }
    }

    private void generateDominateInfo(IRFunction irFunction) {
        // 初始化
        for (IRBasicBlock basicBlock : irFunction.basicBlocks()) {
            if (Objects.equals(basicBlock, irFunction.basicBlocks().get(0))) {
                basicBlock.dominators().add(basicBlock);
            } else {
                basicBlock.dominators().addAll(irFunction.basicBlocks());
            }
        }
        // 迭代计算各个基本块的支配者
        boolean hasUpdates = true;
        while (hasUpdates) {
            hasUpdates = false;
            for (IRBasicBlock basicBlock : irFunction.basicBlocks()) {
                HashSet<IRBasicBlock> newDominators = new HashSet<>();
                // 求交集
                // 这样速度会慢，因为首先计算了并集
                basicBlock.predecessors().forEach((block) -> newDominators.addAll(block.dominators()));
                basicBlock.predecessors().forEach((block) -> newDominators.retainAll(block.dominators()));
                newDominators.add(basicBlock);
                basicBlock.dominators().retainAll(newDominators);
            }
        }
        // 去掉自身，得到每个基本块严格支配者
        // 同时计算各个基本块严格支配的基本块
        for (IRBasicBlock basicBlock : irFunction.basicBlocks()) {
            basicBlock.dominators().remove(basicBlock);
            basicBlock.dominators().forEach((dominator) -> dominator.dominating().add(basicBlock));
        }
        // 计算直接支配者
        for (IRBasicBlock basicBlock : irFunction.basicBlocks()) {
            for (IRBasicBlock dominator : basicBlock.dominators()) {
                // 每个节点的直接支配者有且只有一个
                boolean isImmediateDominator = true;
                // 分析这个支配者支配的所有基本块
                for (IRBasicBlock dominating : dominator.dominating()) {
                    // 如果支配了其他【计算直接支配者的基本块】的支配者，那么就不是直接支配者
                    if (basicBlock.dominators().contains(dominating)) {
                        isImmediateDominator = false;
                        break;
                    }
                }
                if (isImmediateDominator) {
                    basicBlock.setImmediateDominator(dominator);
                    break;
                }
            }
            if (basicBlock.immediateDominator() != null) {
                basicBlock.immediateDominator().immediateDominating().add(basicBlock);
            }
        }
        // 计算支配边界
        for (IRBasicBlock predecessor : irFunction.basicBlocks()) {
            for (IRBasicBlock successor : predecessor.successors()) {
                IRBasicBlock block = predecessor;
                while (!block.dominating().contains(successor)) {
                    block.dominanceFrontiers().add(successor);
                    block = block.immediateDominator();
                    if (block == null) {
                        break;
                    }
                }
            }
        }
    }
}
