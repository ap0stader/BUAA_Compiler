package pass.refactor;

import IR.IRModule;
import IR.value.IRBasicBlock;
import IR.value.IRFunction;
import pass.Pass;

import java.util.*;

public class RemoveUnreachableBasicBlock implements Pass {
    private final IRModule irModule;
    private boolean finished = false;

    public RemoveUnreachableBasicBlock(IRModule irModule) {
        this.irModule = irModule;
    }

    @Override
    public void run() {
        if (finished) {
            throw new RuntimeException("When RemoveUnreachableBasicBlock.run(), the pass is finished");
        }
        for (IRFunction irFunction : irModule.functions()) {
            if (!irFunction.isLib()) {
                this.removeUnreachableBasicBlock(irFunction);
            }
        }
        this.finished = true;
    }

    private void removeUnreachableBasicBlock(IRFunction irFunction) {
        // 消除除了参数块外的所有没有前驱的基本块
        Iterator<IRBasicBlock> iterator = irFunction.basicBlocks().iterator();
        while (iterator.hasNext()) {
            IRBasicBlock basicBlock = iterator.next();
            if (!Objects.equals(basicBlock, irFunction.basicBlocks().get(0)) &&
                    basicBlock.predecessors().isEmpty()) {
                basicBlock.eraseAllInstruction();
                basicBlock.successors().forEach(block -> block.predecessors().remove(basicBlock));
                iterator.remove();
            }
        }
        // 消除无法从函数入口到达的基本块
        // 如果把所有的以ret结尾的块都消除了，说明编译时识别出了死循环
        HashMap<IRBasicBlock, Boolean> dfsVisit = new HashMap<>();
        irFunction.basicBlocks().forEach(block -> dfsVisit.put(block, false));
        LinkedList<IRBasicBlock> dfsStack = new LinkedList<>();
        dfsStack.push(irFunction.basicBlocks().get(0));
        dfsVisit.put(irFunction.basicBlocks().get(0), true);
        while (!dfsStack.isEmpty()) {
            IRBasicBlock currentBasicBlock = dfsStack.pop();
            for (IRBasicBlock successor : currentBasicBlock.successors()) {
                if (!dfsVisit.get(successor)) {
                    dfsStack.push(successor);
                    dfsVisit.put(successor, true);
                }
            }
        }
        for (Map.Entry<IRBasicBlock, Boolean> entry : dfsVisit.entrySet()) {
            if (!entry.getValue()) {
                entry.getKey().eraseAllInstruction();
                entry.getKey().successors().forEach(block -> block.predecessors().remove(entry.getKey()));
            }
        }
        irFunction.basicBlocks().removeIf(basicBlock -> !dfsVisit.get(basicBlock));
    }
}
