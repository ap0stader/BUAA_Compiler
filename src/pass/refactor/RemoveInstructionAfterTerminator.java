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
import java.util.Objects;

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
                for (IRBasicBlock basicBlock : irFunction.basicBlocks()) {
                    this.processBasicBlock(basicBlock);
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
            while (!Objects.equals(irBasicBlock.instructions().tail().value(), terminator)) {
                irBasicBlock.instructions().tail().value().eliminate();
            }
        } else {
            throw new RuntimeException("When RemoveInstructionAfterTerminator.run(), irBasicBlock is not end with a terminator. " +
                    "Got " + irBasicBlock);
        }
    }

    @Override
    public void restart() {
        this.finished = false;
    }
}
