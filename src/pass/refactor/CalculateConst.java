package pass.refactor;

import IR.IRModule;
import IR.value.IRBasicBlock;
import IR.value.IRFunction;
import IR.value.constant.ConstantInt;
import IR.value.instruction.*;
import pass.Pass;
import util.DoublyLinkedList;

public class CalculateConst implements Pass {
    private final IRModule irModule;
    private boolean finished = false;

    public CalculateConst(IRModule irModule) {
        this.irModule = irModule;
    }

    @Override
    public void run() {
        if (finished) {
            throw new RuntimeException("When DeadCodeEmit.run(), the pass is finished");
        }
        for (IRFunction irFunction : irModule.functions()) {
            if (!irFunction.isLib()) {
                while (this.calculateConst(irFunction)) ;
            }
        }
        this.finished = true;
    }

    private boolean calculateConst(IRFunction irFunction) {
        for (IRBasicBlock basicBlock : irFunction.basicBlocks()) {
            for (DoublyLinkedList.Node<IRInstruction<?>> instructionNode : basicBlock.instructions()) {
                if (instructionNode.value() instanceof BinaryOperator binaryOperator &&
                        binaryOperator.getOperand1() instanceof ConstantInt constantInt1 &&
                        binaryOperator.getOperand2() instanceof ConstantInt constantInt2) {
                    ConstantInt constantInt = switch (binaryOperator.binaryOp()) {
                        case ADD ->
                                new ConstantInt(binaryOperator.type(), constantInt1.constantValue() + constantInt2.constantValue());
                        case SUB ->
                                new ConstantInt(binaryOperator.type(), constantInt1.constantValue() - constantInt2.constantValue());
                        case MUL ->
                                new ConstantInt(binaryOperator.type(), constantInt1.constantValue() * constantInt2.constantValue());
                        case DIV ->
                                new ConstantInt(binaryOperator.type(), constantInt1.constantValue() / constantInt2.constantValue());
                        case MOD ->
                                new ConstantInt(binaryOperator.type(), constantInt1.constantValue() % constantInt2.constantValue());
                    };
                    binaryOperator.replaceAllUsesWith(constantInt);
                }
            }
        }
        return false;
    }

    @Override
    public void restart() {
        throw new RuntimeException("DeadCodeEmit can not restart");
    }
}
