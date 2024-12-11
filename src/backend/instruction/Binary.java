package backend.instruction;

import backend.oprand.*;
import backend.target.TargetBasicBlock;

import java.util.Objects;

public class Binary extends TargetInstruction {
    private final BinaryOs operation;
    private TargetRegister destination;
    private TargetRegister registerSource;
    private TargetOperand operandSource;

    public enum BinaryOs {
        // 算数运算
        ADD,
        SUB,
        MUL,
        DIV,
        MOD,
        // 移位运算
        SLL,
        // 比较运算
        SEQ,
        SNE,
        SGT,
        SGE,
        SLT,
        SLE;
    }

    public Binary(TargetBasicBlock targetBasicBlock, BinaryOs operation, TargetOperand destination,
                  TargetOperand operandSourceLeft, TargetOperand operandSourceRight) {
        super(targetBasicBlock);
        if (destination instanceof TargetRegister &&
                ((operandSourceLeft instanceof TargetRegister && operandSourceRight instanceof TargetRegister) ||
                        (operandSourceLeft instanceof TargetRegister && operandSourceRight instanceof Immediate) ||
                        (operandSourceLeft instanceof Immediate && operandSourceRight instanceof TargetRegister))) {
            this.operation = operation;
            this.destination = (TargetRegister) destination;
            if (operandSourceLeft instanceof TargetRegister) {
                this.registerSource = (TargetRegister) operandSourceLeft;
                this.operandSource = operandSourceRight;
            } else {
                this.registerSource = (TargetRegister) operandSourceRight;
                this.operandSource = operandSourceLeft;
            }
            addDef(destination);
            addUse(registerSource);
            addUse(operandSource);
        } else {
            throw new RuntimeException("When Binary(), the type of destination or source is invalid. " +
                    "Got destination: " + destination + ", source: " + operandSourceLeft + ", " + operandSourceRight);
        }
    }

    @Override
    public void replaceDefVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        if (Objects.equals(destination, virtualRegister)) {
            this.destination = physicalRegister;
        } else {
            throw new RuntimeException("When Binary.replaceDefVirtualRegister(), the replaceDefVirtualRegister is not destination");
        }
    }

    @Override
    public void replaceUseVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        if (Objects.equals(registerSource, virtualRegister)) {
            this.registerSource = physicalRegister;
        } else if (Objects.equals(operandSource, virtualRegister)) {
            this.operandSource = physicalRegister;
        } else {
            throw new RuntimeException("When Binary.replaceUseVirtualRegister(), the replaceUseVirtualRegister is not source");
        }
    }

    @Override
    public String mipsStr() {
        if (operandSource instanceof Immediate immediateOperandSource) {
            return switch (this.operation) {
                case ADD ->
                        "addiu " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SUB -> {
                    // MARS的伪指令翻译不合理，使用addiu
                    Immediate subOperandSource = new Immediate(-immediateOperandSource.value());
                    yield "addiu " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + subOperandSource.mipsStr();
                }
                case MUL ->
                        "mul " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                // 对于立即数，即便是0，MARS也不会报错，所以用伪指令
                case DIV ->
                        "div " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case MOD ->
                        "rem " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SLL ->
                        "sll " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SEQ ->
                        "seq " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SNE ->
                        "sne " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SGT ->
                        "sgt " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SGE ->
                        "sge " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SLT ->
                        "slti " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SLE ->
                        "sle " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
            };
        } else { // operandSource instanceof TargetRegister
            return switch (this.operation) {
                case ADD ->
                        "addu " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SUB ->
                        "subu " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case MUL ->
                        "mul " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                // MARS的伪指令有不必要的bne和break，所以不用伪指令
                case DIV -> "div " + registerSource.mipsStr() + ", " + operandSource.mipsStr() + "\n\t" +
                        "mflo " + destination.mipsStr();
                case MOD -> "div " + registerSource.mipsStr() + ", " + operandSource.mipsStr() + "\n\t" +
                        "mfhi " + destination.mipsStr();
                case SLL ->
                        "sllv " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SEQ ->
                        "seq " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SNE ->
                        "sne " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SGT ->
                        "sgt " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SGE ->
                        "sge " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SLT ->
                        "slt " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SLE ->
                        "sle " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
            };
        }
    }
}
