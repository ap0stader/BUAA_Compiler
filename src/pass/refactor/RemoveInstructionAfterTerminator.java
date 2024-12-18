package pass.refactor;

import IR.IRModule;
import IR.value.IRBasicBlock;
import IR.value.IRFunction;
import IR.value.instruction.BranchInst;
import IR.value.instruction.IRInstruction;
import IR.value.instruction.ReturnInst;
import pass.Pass;
import util.DoublyLinkedList;

import java.util.Iterator;

public class RemoveInstructionAfterTerminator implements Pass {
    private final IRModule irModule;
    private boolean finished = false;

    public RemoveInstructionAfterTerminator(IRModule irModule) {
        this.irModule = irModule;
    }

    @Override
    public void run() {
        if (finished) {
            throw new RuntimeException("When RemoveInstructionAfterTerminator.run(), the pass is finished");
        }
        for (IRFunction irFunction : irModule.functions()) {
            if (!irFunction.isLib()) {
                for (int i = 2; i < irFunction.basicBlocks().size(); i++) {
                    this.processBasicBlock(irFunction.basicBlocks().get(i));
                }
            }
        }
        this.finished = true;
    }

    private void processBasicBlock(IRBasicBlock irBasicBlock) {
        Iterator<DoublyLinkedList.Node<IRInstruction<?>>> iterator = irBasicBlock.instructions().iterator();
        IRInstruction<?> terminator = null;
        while (iterator.hasNext()) {
            IRInstruction<?> instruction = iterator.next().value();
            if (instruction instanceof BranchInst || instruction instanceof ReturnInst) {
                terminator = instruction;
                break;
            }
        }
        if (terminator != null) {
            while (terminator.listNode().next() != null) {
                IRInstruction<?> unreachableInstruction = terminator.listNode().next().value();
                unreachableInstruction.eliminate();
            }
        }
    }

    @Override
    public void restart() {
        this.finished = false;
    }
}
