package pass.refactor;

import IR.IRModule;
import IR.IRValue;
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
            throw new RuntimeException("When CalculateConst.run(), the pass is finished");
        }
        for (IRFunction irFunction : irModule.functions()) {
            if (!irFunction.isLib()) {
                //noinspection StatementWithEmptyBody
                while (this.calculateConst(irFunction)) ;
            }
        }
        this.finished = true;
    }

    private boolean calculateConst(IRFunction irFunction) {
        for (IRBasicBlock basicBlock : irFunction.basicBlocks()) {
            for (DoublyLinkedList.Node<IRInstruction<?>> instructionNode : basicBlock.instructions()) {
                if (instructionNode.value() instanceof BinaryOperator binaryOperator) {
                    IRValue<?> replacement = null;

                    if (binaryOperator.getOperand1() instanceof ConstantInt constantInt1 &&
                            binaryOperator.getOperand2() instanceof ConstantInt constantInt2) {
                        replacement = switch (binaryOperator.binaryOp()) {
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
                    } else if (binaryOperator.getOperand1() instanceof ConstantInt constantInt1) {
                        if (constantInt1.constantValue() == 0) {
                            if (binaryOperator.binaryOp() == BinaryOperator.BinaryOps.ADD) {
                                // 0 + x = x
                                replacement = binaryOperator.getOperand2();
                            } else if (binaryOperator.binaryOp() == BinaryOperator.BinaryOps.MUL ||
                                    binaryOperator.binaryOp() == BinaryOperator.BinaryOps.DIV ||
                                    binaryOperator.binaryOp() == BinaryOperator.BinaryOps.MOD) {
                                // 0 * x = 0
                                // 0 / x = 0 (此时x未知是否为0，但是除0是未定义行为，可直接优化掉）
                                // 0 % x = 0 (此时x未知是否为0，但是模0是未定义行为，可直接优化掉）
                                replacement = new ConstantInt(binaryOperator.type(), 0);
                            }
                        } else if (constantInt1.constantValue() == 1) {
                            if (binaryOperator.binaryOp() == BinaryOperator.BinaryOps.MUL) {
                                // 1 * x = x
                                replacement = binaryOperator.getOperand2();
                            }
                        }
                    } else if (binaryOperator.getOperand2() instanceof ConstantInt constantInt2) {
                        if (constantInt2.constantValue() == 0) {
                            if (binaryOperator.binaryOp() == BinaryOperator.BinaryOps.ADD ||
                                binaryOperator.binaryOp() == BinaryOperator.BinaryOps.SUB) {
                                // x + 0 = x
                                // x - 0 = x
                                replacement = binaryOperator.getOperand1();
                            } else if (binaryOperator.binaryOp() == BinaryOperator.BinaryOps.MUL) {
                                // x * 0 = 0
                                replacement = new ConstantInt(binaryOperator.type(), 0);
                            }
                        } else if (constantInt2.constantValue() == 1) {
                            if (binaryOperator.binaryOp() == BinaryOperator.BinaryOps.MUL) {
                                // x * 1 = x
                                replacement = binaryOperator.getOperand1();
                            }
                        }
                    }

                    if (replacement != null && !binaryOperator.users().isEmpty()) {
                        binaryOperator.replaceAllUsesWith(replacement);
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
