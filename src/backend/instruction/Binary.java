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
        // 按位运算
        AND,
        // 移位运算
        SLL,
        SRA,
        SRL,
        // 比较运算
        SEQ,
        SNE,
        SGT,
        SGE,
        SLT,
        SLE,
        // 除法优化
        HIMULT,
        HIMADD,
    }

    public Binary(TargetBasicBlock targetBasicBlock, BinaryOs operation, TargetOperand destination,
                  TargetOperand registerSource, TargetOperand operandSource) {
        super(targetBasicBlock);
        if (destination instanceof TargetRegister destinationRegister
                && registerSource instanceof TargetRegister registerSourceRegister
                && (operandSource instanceof TargetRegister || operandSource instanceof Immediate)) {
            // CAST 上方的instanceof确保转换正确
            this.operation = operation;
            this.destination = destinationRegister;
            this.registerSource = registerSourceRegister;
            this.operandSource = operandSource;
            addDef(this.destination);
            addUse(this.registerSource);
            addUse(this.operandSource);
        } else {
            throw new RuntimeException("When Binary(), the type of destination or source is invalid. " +
                    "Got destination: " + destination + ", source: " + registerSource + ", " + operandSource);
        }
    }

    @Override
    public void replaceDefVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        if (Objects.equals(destination, virtualRegister)) {
            this.destination = physicalRegister;
        }
    }

    @Override
    public void replaceUseVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        if (Objects.equals(registerSource, virtualRegister)) {
            this.registerSource = physicalRegister;
        }
        if (Objects.equals(operandSource, virtualRegister)) {
            this.operandSource = physicalRegister;
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
                case AND ->
                        "andi " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SLL ->
                        "sll " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SRA ->
                        "sra " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SRL ->
                        "srl " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
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
                default -> throw new RuntimeException("When Binary(), the type of destination or source is invalid");
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
                case AND ->
                        "and " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SLL ->
                        "sllv " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SRA ->
                        "srav " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
                case SRL ->
                        "srlv " + destination.mipsStr() + ", " + registerSource.mipsStr() + ", " + operandSource.mipsStr();
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
                case HIMULT -> "mult " + registerSource.mipsStr() + ", " + operandSource.mipsStr() + "\n\t" +
                        "mfhi " + destination.mipsStr();
                case HIMADD -> "mthi " + registerSource.mipsStr() + "\n\t" +
                        "madd " + registerSource.mipsStr() + ", " + operandSource.mipsStr() + "\n\t" +
                        "mfhi " + destination.mipsStr();
            };
        }
    }
}
