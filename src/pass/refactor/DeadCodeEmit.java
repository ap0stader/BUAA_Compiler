package pass.refactor;

import IR.IRModule;
import IR.value.IRBasicBlock;
import IR.value.IRFunction;
import IR.value.instruction.*;
import pass.Pass;
import util.DoublyLinkedList;

import java.util.HashSet;

public class DeadCodeEmit implements Pass {
    private final IRModule irModule;
    private boolean finished = false;

    private final HashSet<IRInstruction<?>> liveInstruction;

    public DeadCodeEmit(IRModule irModule) {
        this.irModule = irModule;
        this.liveInstruction = new HashSet<>();
    }

    @Override
    public void run() {
        if (finished) {
            throw new RuntimeException("When DeadCodeEmit.run(), the pass is finished");
        }
        for (IRFunction irFunction : irModule.functions()) {
            if (!irFunction.isLib()) {
                this.deadCodeEmit(irFunction);
            }
        }
        this.finished = true;
    }

    private static boolean isLive(IRInstruction<?> irInstruction) {
        return irInstruction instanceof BranchInst ||
                irInstruction instanceof ReturnInst ||
                irInstruction instanceof StoreInst ||
                irInstruction instanceof CallInst;
    }

    private void deadCodeEmit(IRFunction irFunction) {
        this.liveInstruction.clear();
        for (IRBasicBlock basicBlock : irFunction.basicBlocks()) {
            DoublyLinkedList.Node<IRInstruction<?>> instructionNode = basicBlock.instructions().tail();
            while (instructionNode != null) {
                if (isLive(instructionNode.value())) {
                    this.calculateClosure(instructionNode.value());
                }
                instructionNode = instructionNode.pred();
            }
        }

        for (IRBasicBlock basicBlock : irFunction.basicBlocks()) {
            DoublyLinkedList.Node<IRInstruction<?>> instructionNode = basicBlock.instructions().tail();
            while (instructionNode != null) {
                IRInstruction<?> instruction = instructionNode.value();
                if (!this.liveInstruction.contains(instruction)) {
                    instruction.eliminateWithoutCheck();
                }
                instructionNode = instructionNode.pred();
            }
        }
    }

    private void calculateClosure(IRInstruction<?> irInstruction) {
        if (!this.liveInstruction.contains(irInstruction)) {
            this.liveInstruction.add(irInstruction);
            for (int i = 0; i < irInstruction.getNumOperands(); i++) {
                if (irInstruction.getOperand(i) instanceof IRInstruction<?> usedInstruction) {
                    this.calculateClosure(usedInstruction);
                }
            }
        }
    }
}
