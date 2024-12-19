package backend;

import IR.IRModule;
import IR.IRValue;
import IR.type.*;
import IR.value.*;
import IR.value.constant.*;
import IR.value.instruction.*;
import backend.instruction.*;
import backend.oprand.*;
import backend.target.*;
import util.DoublyLinkedList;
import util.Pair;

import java.util.*;

public class Generator {
    private final IRModule irModule;
    private boolean finish = false;

    // 存储IR的GlobalVariable与Target的Address之间的对应关系，在加载全局变量时使用
    private final HashMap<IRGlobalVariable, LabelBaseAddress> globalVariableMap;
    // 存储IR的Function与Target的Function之间的对应关系，在函数调用处使用
    private final HashMap<IRFunction, TargetFunction> functionMap;
    // 存储是库函数的IR的Function与系统调用号之间的对应关系，在函数调用出使用
    private final HashMap<IRFunction, Integer> syscallMap;
    // 存储一个函数内IR的BasicBlock与Target的BasicBlock之间的对应关系，在跳转指令处使用
    private final HashMap<IRBasicBlock, TargetBasicBlock> basicBlockMap;
    // 存疑一个函数内Target的BasicBlock与实际要跳转到的Label之间的对应管理，处理PHI指令时记录，在跳转指令处使用
    private final HashMap<TargetBasicBlock, HashMap<TargetBasicBlock, TargetBasicBlock>> basicBlockBranchMap;
    // 存储一个函数内Target的BasicBlock中还未释放的消除PHI指令的Move，在跳转指令处释放
    private final HashMap<TargetBasicBlock, LinkedList<Pair<VirtualRegister, TargetOperand>>> basicBlockPhiCopies;
    // 存储一个函数内IR的Value与Target的Operand之间的对应关系
    private final HashMap<IRValue<?>, TargetOperand> valueMap;

    private final TargetModule targetModule;

    public Generator(IRModule irModule) {
        this.irModule = irModule;
        this.targetModule = new TargetModule();
        this.globalVariableMap = new HashMap<>();
        this.functionMap = new HashMap<>();
        this.syscallMap = new HashMap<>();
        this.basicBlockMap = new HashMap<>();
        this.basicBlockBranchMap = new HashMap<>();
        this.basicBlockPhiCopies = new HashMap<>();
        this.valueMap = new HashMap<>();
    }

    public TargetModule generateTargetModule() {
        if (finish) {
            return this.targetModule;
        }
        // IRGlobalVariable转换为.data段
        this.irModule.globalVariables().forEach(this::transformIRGlobalVariable);
        // IRFunction转换为.text段
        this.irModule.functions().forEach(this::transformIRFunction);
        this.finish = true;
        return this.targetModule;
    }

    private TargetOperand valueToOperand(IRValue<?> irValue) {
        if (irValue instanceof ConstantInt constantInt) {
            return new Immediate(constantInt.constantValue());
        } else if (irValue instanceof IRGlobalVariable) {
            return this.globalVariableMap.get(irValue);
        } else if (irValue instanceof IRFunction) {
            return this.functionMap.get(irValue).label();
        } else if (irValue instanceof IRBasicBlock) {
            return this.basicBlockMap.get(irValue).label();
        } else if (irValue instanceof IRInstruction) {
            return this.valueMap.get(irValue);
        } else {
            throw new RuntimeException("When valueToOperand(), the type of irValue is invalid. Got " + irValue);
        }
    }

    private void transformIRGlobalVariable(IRGlobalVariable irGlobalVariable) {
        TargetDataObject targetDataObject = new TargetDataObject(irGlobalVariable.name());
        this.globalVariableMap.put(irGlobalVariable, targetDataObject.address());
        if (irGlobalVariable.initVals() instanceof ConstantInt initInt) {
            targetDataObject.appendData(initInt);
        } else if (irGlobalVariable.initVals() instanceof ConstantArray initArray
                && initArray.type().elementType() instanceof IntegerType) {
            // CAST 上方的instanceof确保转换正确
            initArray.constantValues().forEach((V) -> targetDataObject.appendData((ConstantInt) V));
        } else if (irGlobalVariable.initVals() instanceof ConstantAggregateZero initAggregateZero
                && initAggregateZero.type() instanceof ArrayType initZeroArrayType
                && initZeroArrayType.elementType() instanceof IntegerType initZeroArrayElementType) {
            targetDataObject.appendZero(initZeroArrayElementType, initZeroArrayType.numElements());
        } else if (irGlobalVariable.initVals() instanceof ConstantStruct initStruct) {
            // 由于StructType的IRGlobalVariable只会因为长数组的优化产生，故按照长数组优化形成的结构解析
            for (IRConstant<?> initValue : initStruct.constantValues()) {
                if (initValue instanceof ConstantInt initInt) {
                    targetDataObject.appendData(initInt);
                } else if (initValue instanceof ConstantAggregateZero initAggregateZero
                        && initAggregateZero.type() instanceof ArrayType initZeroArrayType
                        && initZeroArrayType.elementType() instanceof IntegerType initZeroArrayElementType) {
                    targetDataObject.appendZero(initZeroArrayElementType, initZeroArrayType.numElements());
                } else {
                    throw new RuntimeException("When transformIRGlobalVariable(), the member of ConstantStruct initVals of IRGlobalVariable is invalid. " +
                            "Got " + initValue.type());
                }
            }
        } else {
            throw new RuntimeException("When transformIRGlobalVariable(), the initVals type of IRGlobalVariable is invalid. " +
                    "Got " + irGlobalVariable.initVals().type());
        }
        this.targetModule.appendDataObjects(targetDataObject);
    }

    private void transformIRFunction(IRFunction irFunction) {
        // 库函数为输入输出函数，使用syscall模板处理，不做解析
        if (irFunction.isLib()) {
            this.syscallMap.put(irFunction, switch (irFunction.name()) {
                case "getint" -> 5;
                case "getchar" -> 12;
                case "putint" -> 1;
                case "putch" -> 11;
                case "putstr" -> 4;
                default ->
                        throw new RuntimeException("When transformIRFunction(), the library function is unimplemented. " +
                                "Got " + irFunction);
            });
        } else {
            TargetFunction targetFunction = new TargetFunction(irFunction.name());
            this.functionMap.put(irFunction, targetFunction);
            // 局限于一个函数的数据，新的一个函数需要清空
            this.basicBlockMap.clear();
            this.valueMap.clear();
            this.basicBlockMap.clear();
            this.basicBlockBranchMap.clear();
            this.basicBlockPhiCopies.clear();
            if (irFunction.basicBlocks().size() < 3) {
                throw new RuntimeException("When transformIRFunction(), the basic block of function " + irFunction.name() +
                        " is less than requirements.");
            }
            this.transformFunctionArgBlock(irFunction.basicBlocks().get(0), irFunction, targetFunction);
            this.transformFunctionDefBlock(irFunction.basicBlocks().get(1), irFunction, targetFunction);
            // 先创建好一个函数的所有的TargetBasicBlock
            Iterator<IRBasicBlock> basicBlockIterator;
            basicBlockIterator = irFunction.getIteratorFromStartBlock();
            int basicBlockOrder = 0;
            while (basicBlockIterator.hasNext()) {
                IRBasicBlock irBasicBlock = basicBlockIterator.next();
                TargetBasicBlock targetBasicBlock = new TargetBasicBlock(targetFunction, basicBlockOrder++);
                this.basicBlockMap.put(irBasicBlock, targetBasicBlock);
                this.basicBlockBranchMap.put(targetBasicBlock, new HashMap<>());
                targetFunction.appendBasicBlock(targetBasicBlock.listNode());
            }
            // 计算TargetBasicBlock的前驱和后驱信息
            basicBlockIterator = irFunction.getIteratorFromStartBlock();
            while (basicBlockIterator.hasNext()) {
                IRBasicBlock irBasicBlock = basicBlockIterator.next();
                TargetBasicBlock targetBasicBlock = this.basicBlockMap.get(irBasicBlock);
                if (targetBasicBlock.order() != 0) {
                    // 开始基本块在MIPS中无前驱基本块
                    targetBasicBlock.predecessors().addAll(irBasicBlock.predecessors().stream().map(this.basicBlockMap::get).toList());
                }
                targetBasicBlock.successors().addAll(irBasicBlock.successors().stream().map(this.basicBlockMap::get).toList());
            }
            // 处理PHI指令
            basicBlockIterator = irFunction.getIteratorFromStartBlock();
            while (basicBlockIterator.hasNext()) {
                IRBasicBlock irBasicBlock = basicBlockIterator.next();
                TargetBasicBlock targetBasicBlock = this.basicBlockMap.get(irBasicBlock);
                if (targetBasicBlock.order() != 0) {
                    // 开始基本块在MIPS中无前驱基本块，也没有PHI指令需要处理
                    this.transformIRBasicBlockPHI(irBasicBlock, targetBasicBlock, targetFunction);
                }
            }
            // 处理剩余指令
            basicBlockIterator = irFunction.getIteratorFromStartBlock();
            while (basicBlockIterator.hasNext()) {
                IRBasicBlock irBasicBlock = basicBlockIterator.next();
                TargetBasicBlock targetBasicBlock = this.basicBlockMap.get(irBasicBlock);
                this.transformIRBasicBlock(irBasicBlock, targetBasicBlock);
            }
            this.targetModule.appendFunctions(targetFunction);
        }
    }

    private void transformFunctionArgBlock(IRBasicBlock argBlock, IRFunction irFunction, TargetFunction targetFunction) {
        ArrayList<Argument> arguments = irFunction.arguments();
        // 为第0-3个参数登记对应的寄存器
        for (int i = 0; i < arguments.size(); i++) {
            if (PhysicalRegister.argumentRegisterOfArgumentNumber(i) != null) {
                this.valueMap.put(arguments.get(i), PhysicalRegister.argumentRegisterOfArgumentNumber(i));
            }
        }
        DoublyLinkedList<IRInstruction<?>> instructions = argBlock.instructions();
        // argBlock的规范是 一条alloca指令配上一条将参数写入alloca地址
        Iterator<DoublyLinkedList.Node<IRInstruction<?>>> argBlockIterator = instructions.iterator();
        while (argBlockIterator.hasNext()) {
            IRInstruction<?> irInstructionFirst = argBlockIterator.next().value();
            if (irInstructionFirst instanceof AllocaInst allocaInst && argBlockIterator.hasNext()) {
                IRInstruction<?> irInstructionSecond = argBlockIterator.next().value();
                if (irInstructionSecond instanceof StoreInst storeInst
                        // storeInst与allocaInst是搭配的
                        && storeInst.getValueOperand() instanceof Argument argument
                        && storeInst.getPointerOperand() == allocaInst) {
                    int argumentNumber = arguments.indexOf(argument);
                    // 如果是通过寄存器传递的参数，那么要将寄存器保存到栈上合理的位置
                    if (PhysicalRegister.argumentRegisterOfArgumentNumber(argumentNumber) != null) {
                        targetFunction.stackFrame.ensureSaveRegister(PhysicalRegister.argumentRegisterOfArgumentNumber(argumentNumber));
                    }
                    this.valueMap.put(allocaInst, targetFunction.stackFrame.getInArgumentAddress(argumentNumber));
                    // 该段argBlock正确结束
                    continue;
                }
            } else if (irInstructionFirst instanceof BranchInst && !argBlockIterator.hasNext()) {
                // argBlock正确结束
                break;
            }
            // argBlock不符合约定
            throw new RuntimeException("When transformFunctionArgBlock(), the argBlock of function " + irFunction.name() + " is invalid");
        }
    }

    private void transformFunctionDefBlock(IRBasicBlock defBlock, IRFunction irFunction, TargetFunction targetFunction) {
        DoublyLinkedList<IRInstruction<?>> instructions = defBlock.instructions();
        // defBlock的规范是 均为alloca指令
        Iterator<DoublyLinkedList.Node<IRInstruction<?>>> defBlockIterator = instructions.iterator();
        while (defBlockIterator.hasNext()) {
            IRInstruction<?> irInstruction = defBlockIterator.next().value();
            if (irInstruction instanceof AllocaInst allocaInst) {
                if (allocaInst.allocatedType() instanceof IntegerType allocIntegerType
                        && (IRType.isEqual(allocIntegerType, IRType.getInt8Ty()) || IRType.isEqual(allocIntegerType, IRType.getInt32Ty()))) {
                    this.valueMap.put(allocaInst, targetFunction.stackFrame.alloc(
                            allocIntegerType.getByteWidth()));
                    continue;
                } else if (allocaInst.allocatedType() instanceof ArrayType allocArrayType
                        && allocArrayType.elementType() instanceof IntegerType allocArrayElementType
                        && (IRType.isEqual(allocArrayElementType, IRType.getInt8Ty()) || IRType.isEqual(allocArrayElementType, IRType.getInt32Ty()))) {
                    this.valueMap.put(allocaInst, targetFunction.stackFrame.alloc(
                            allocArrayType.numElements() * allocArrayElementType.getByteWidth()));
                    continue;
                } else {
                    throw new RuntimeException("When transformFunctionDefBlock(), the allocaInst of allocaInst " + allocaInst + " is invalid. " +
                            "Got " + allocaInst.allocatedType());
                }
            } else if (irInstruction instanceof BranchInst && !defBlockIterator.hasNext()) {
                // defBlock正确结束
                break;
            }
            // defBlock不符合约定
            throw new RuntimeException("When transformFunctionDefBlock(), the defBlock of function " + irFunction.name() + " is invalid");
        }
    }

    private void transformIRBasicBlockPHI(IRBasicBlock irBasicBlock, TargetBasicBlock targetBasicBlock, TargetFunction targetFunction) {
        HashMap<IRBasicBlock, LinkedList<Pair<VirtualRegister, TargetOperand>>> phiCopies = new HashMap<>();
        // 初始化
        irBasicBlock.predecessors().forEach((block -> phiCopies.put(block, new LinkedList<>())));
        // 计算需要Copy的内容，目前是并行的版本
        for (DoublyLinkedList.Node<IRInstruction<?>> instructionNode : irBasicBlock.instructions()) {
            if (instructionNode.value() instanceof PHINode phiNode) {
                VirtualRegister phiVirtualRegister = this.getOrGenVirtualRegister(phiNode, targetFunction);
                for (Pair<IRBasicBlock, IRValue<?>> blockValuePair : phiNode.getIncomingBlockValuePairs()) {
                    if (blockValuePair.value() instanceof IRInstruction<?> irInstruction) {
                        this.getOrGenVirtualRegister(irInstruction, targetFunction);
                    }
                    phiCopies.get(blockValuePair.key()).add(new Pair<>(phiVirtualRegister, this.valueToOperand(blockValuePair.value())));
                }
            } else {
                // PHINode集中在IR基本块的开始处
                break;
            }
        }
        // 将并行的Copy修改为串行的Copy
        HashMap<IRBasicBlock, LinkedList<Pair<VirtualRegister, TargetOperand>>> phiCopiesSequential = new HashMap<>();
        // 初始化
        irBasicBlock.predecessors().forEach((block -> phiCopiesSequential.put(block, new LinkedList<>())));
        for (Map.Entry<IRBasicBlock, LinkedList<Pair<VirtualRegister, TargetOperand>>> phiCopyEntry : phiCopies.entrySet()) {
            Iterator<Pair<VirtualRegister, TargetOperand>> phiCopyIterator = phiCopyEntry.getValue().iterator();
            while (phiCopyIterator.hasNext()) {
                Pair<VirtualRegister, TargetOperand> phiCopyMove = phiCopyIterator.next();
                if (!Objects.equals(phiCopyMove.key(), phiCopyMove.value())) {
                    // MAYBE 如果Move起点终点相同可以消去
                    boolean crash = false;
                    for (Pair<VirtualRegister, TargetOperand> otherPhiCopyMove : phiCopyEntry.getValue()) {
                        if (Objects.equals(phiCopyMove.key(), otherPhiCopyMove.value())) {
                            // 存在冲突
                            crash = true;
                            break;
                        }
                    }
                    if (crash) {
                        VirtualRegister transitiveVirtualRegister = targetFunction.addVirtualRegister();
                        // 增加临时Move
                        phiCopiesSequential.get(phiCopyEntry.getKey()).push(new Pair<>(transitiveVirtualRegister, phiCopyMove.value()));
                        phiCopiesSequential.get(phiCopyEntry.getKey()).add(new Pair<>(phiCopyMove.key(), transitiveVirtualRegister));
                    } else {
                        // 直接采用当前Move
                        phiCopiesSequential.get(phiCopyEntry.getKey()).add(new Pair<>(phiCopyMove.key(), phiCopyMove.value()));
                    }
                }
                phiCopyIterator.remove();
            }
        }
        // 保留Copy或直接插入
        for (IRBasicBlock predecessor : irBasicBlock.predecessors()) {
            TargetBasicBlock targetPredecessorBlock = this.basicBlockMap.get(predecessor);
            if (predecessor.successors().size() <= 1) {
                // 只有一个出边
                // 如果只有一个出边，那么也只会被调用一次
                this.basicBlockBranchMap.get(targetPredecessorBlock).put(targetBasicBlock, targetBasicBlock);
                this.basicBlockPhiCopies.put(targetPredecessorBlock, phiCopiesSequential.get(predecessor));
            } else {
                // 有多个出边，增加一个Target的基本块
                // 每次调用都是增加新的内容
                if (!phiCopiesSequential.get(predecessor).isEmpty()) {
                    TargetBasicBlock transitiveBasicBlock = new TargetBasicBlock(targetBasicBlock.parent(),
                            targetPredecessorBlock.order(), targetBasicBlock.order());
                    transitiveBasicBlock.listNode().insertAfter(targetPredecessorBlock.listNode());
                    for (Pair<VirtualRegister, TargetOperand> phyCopyMove : phiCopiesSequential.get(predecessor)) {
                        new Move(transitiveBasicBlock, phyCopyMove.key(), phyCopyMove.value());
                    }
                    new Branch(transitiveBasicBlock, targetBasicBlock.label(), false);
                    this.basicBlockBranchMap.get(targetPredecessorBlock).put(targetBasicBlock, transitiveBasicBlock);
                } else {
                    this.basicBlockBranchMap.get(targetPredecessorBlock).put(targetBasicBlock, targetBasicBlock);
                }
            }
        }
    }

    // 用于动态决定是否分配寄存器，因为PHI是乱序的
    private VirtualRegister getOrGenVirtualRegister(IRInstruction<?> irInstruction, TargetFunction targetFunction) {
        if (this.valueMap.containsKey(irInstruction)) {
            // CAST 生成规范限制
            return (VirtualRegister) this.valueMap.get(irInstruction);
        } else {
            VirtualRegister virtualRegister = targetFunction.addVirtualRegister();
            this.valueMap.put(irInstruction, virtualRegister);
            return virtualRegister;
        }
    }

    private void transformIRBasicBlock(IRBasicBlock irBasicBlock, TargetBasicBlock targetBasicBlock) {
        DoublyLinkedList<IRInstruction<?>> instructions = irBasicBlock.instructions();
        for (DoublyLinkedList.Node<IRInstruction<?>> irInstructionNode : instructions) {
            IRInstruction<?> irInstruction = irInstructionNode.value();
            if (irInstruction instanceof BinaryOperator binaryOperator) {
                this.transformBinaryOperator(binaryOperator, targetBasicBlock);
            } else if (irInstruction instanceof IcmpInst icmpInst) {
                this.transformIcmpInst(icmpInst, targetBasicBlock);
            } else if (irInstruction instanceof CastInst<?> castInst) {
                this.transformCastInst(castInst, targetBasicBlock);
            } else if (irInstruction instanceof GetElementPtrInst getElementPtrInst) {
                this.transformGetElementPtrInst(getElementPtrInst, targetBasicBlock);
            } else if (irInstruction instanceof LoadInst loadInst) {
                this.transformLoadInst(loadInst, targetBasicBlock);
            } else if (irInstruction instanceof StoreInst storeInst) {
                this.transformStoreInst(storeInst, targetBasicBlock);
            } else if (irInstruction instanceof CallInst callInst) {
                this.transformCallInst(callInst, targetBasicBlock);
            } else if (irInstruction instanceof BranchInst branchInst) {
                // BranchInst是基本块的终结符
                this.releasePhiCopies(targetBasicBlock);
                this.transformBranchInst(branchInst, targetBasicBlock);
            } else if (irInstruction instanceof ReturnInst returnInst) {
                this.transformReturnInst(returnInst, targetBasicBlock);
            } else if (irInstruction instanceof PHINode) {
                continue;
            } else {
                throw new RuntimeException("When transformIRBasicBlock(), the type of irInstruction is unsupported in a normal basic block");
            }
        }
    }

    private void releasePhiCopies(TargetBasicBlock targetBasicBlock) {
        if (this.basicBlockPhiCopies.containsKey(targetBasicBlock)) {
            for (Pair<VirtualRegister, TargetOperand> phyCopyMove : this.basicBlockPhiCopies.get(targetBasicBlock)) {
                new Move(targetBasicBlock, phyCopyMove.key(), phyCopyMove.value());
            }
        }
    }

    private static int tailZero(Integer i) {
        int result = -1;
        i = i >>> 1;
        do {
            i = i >>> 1;
            result++;
        }
        while (i != 0);
        return result;
    }

    private void transformDivConst(VirtualRegister destinationRegister, TargetOperand registerSource,
                                   Integer divisor, TargetBasicBlock targetBasicBlock) {
        // 相反
        boolean isNegativeDivisor = divisor < 0;
        // 除-1和除1
        if (divisor == -1) {
            new Binary(targetBasicBlock, Binary.BinaryOs.SUB, destinationRegister, PhysicalRegister.ZERO, registerSource);
            return;
        } else if (divisor == 1) {
            new Move(targetBasicBlock, destinationRegister, registerSource);
            return;
        }
        divisor = Math.abs(divisor);
        // dst = dividend / abs => dst = (dividend * n) >> shift
        // nc = 2^31 - 2^31 % abs - 1
        long nc = ((long) 1 << 31) - (((long) 1 << 31) % divisor) - 1;
        long p = 32;
        // 2^p > (2^31 - 2^31 % abs - 1) * (abs - 2^p % abs)
        while (((long) 1 << p) <= nc * (divisor - ((long) 1 << p) % divisor)) {
            p++;
        }
        // m = (2^p + abs - 2^p % abs) / abs
        long m = ((((long) 1 << p) + (long) divisor - ((long) 1 << p) % divisor) / (long) divisor);
        int n = (int) ((m << 32) >>> 32);
        int shift = (int) (p - 32);

        VirtualRegister multDivisor = targetBasicBlock.parent().addVirtualRegister();
        new Move(targetBasicBlock, multDivisor, new Immediate(n));

        VirtualRegister multTemp = targetBasicBlock.parent().addVirtualRegister();
        if (m >= 0x80000000L) {
            // multTemp = dividend + (dividend * n)[63:32]
            new Binary(targetBasicBlock, Binary.BinaryOs.HIMADD, multTemp, registerSource, multDivisor);
        }
        else {
            // multTemp = (dividend * n)[63:32]
            new Binary(targetBasicBlock, Binary.BinaryOs.HIMUL, multTemp, registerSource, multDivisor);
        }

        // shiftShift = temp >> shift
        VirtualRegister multShiftShift = targetBasicBlock.parent().addVirtualRegister();
        new Binary(targetBasicBlock, Binary.BinaryOs.SRA, multShiftShift, multTemp, new Immediate(shift));

        // dst = shiftShift + dividend >> 31
        VirtualRegister multShift32 = targetBasicBlock.parent().addVirtualRegister();
        new Binary(targetBasicBlock, Binary.BinaryOs.SRL, multShift32, registerSource, new Immediate(31));
        new Binary(targetBasicBlock, Binary.BinaryOs.ADD, destinationRegister, multShiftShift, multShift32);

        if (isNegativeDivisor) {
            new Binary(targetBasicBlock, Binary.BinaryOs.SUB, destinationRegister, PhysicalRegister.ZERO, registerSource);
        }
    }

    private void transformBinaryOperator(BinaryOperator binaryOperator, TargetBasicBlock targetBasicBlock) {
        IRValue<IntegerType> operandLeft = binaryOperator.getOperand1();
        IRValue<IntegerType> operandRight = binaryOperator.getOperand2();
        VirtualRegister destinationRegister = this.getOrGenVirtualRegister(binaryOperator, targetBasicBlock.parent());
        Binary.BinaryOs targetBinaryOps = switch (binaryOperator.binaryOp()) {
            case ADD -> Binary.BinaryOs.ADD;
            case SUB -> Binary.BinaryOs.SUB;
            case MUL -> Binary.BinaryOs.MUL;
            case DIV -> Binary.BinaryOs.DIV;
            case MOD -> Binary.BinaryOs.MOD;
        };
        TargetOperand registerSource;
        if (this.valueToOperand(operandLeft) instanceof Immediate) {
            // 左侧的Immediate要先进入寄存器
            registerSource = targetBasicBlock.parent().addVirtualRegister();
            new Move(targetBasicBlock, registerSource, this.valueToOperand(operandLeft));
        } else {
            registerSource = this.valueToOperand(operandLeft);
        }
        if (targetBinaryOps == Binary.BinaryOs.DIV && operandRight instanceof ConstantInt rightConstant) {
            this.transformDivConst(destinationRegister, registerSource, rightConstant.constantValue(), targetBasicBlock);
        } else {
            new Binary(targetBasicBlock, targetBinaryOps, destinationRegister, registerSource, this.valueToOperand(operandRight));
        }
    }

    private void transformIcmpInst(IcmpInst icmpInst, TargetBasicBlock targetBasicBlock) {
        IRValue<IntegerType> operandLeft = icmpInst.getOperand1();
        IRValue<IntegerType> operandRight = icmpInst.getOperand2();
        VirtualRegister destinationRegister = this.getOrGenVirtualRegister(icmpInst, targetBasicBlock.parent());
        Binary.BinaryOs targetBinaryOps = switch (icmpInst.predicate()) {
            case EQ -> Binary.BinaryOs.SEQ;
            case NE -> Binary.BinaryOs.SNE;
            case GT -> Binary.BinaryOs.SGT;
            case GE -> Binary.BinaryOs.SGE;
            case LT -> Binary.BinaryOs.SLT;
            case LE -> Binary.BinaryOs.SLE;
        };
        TargetOperand registerSource;
        if (this.valueToOperand(operandLeft) instanceof Immediate) {
            // 左侧的Immediate要先进入寄存器
            registerSource = targetBasicBlock.parent().addVirtualRegister();
            new Move(targetBasicBlock, registerSource, this.valueToOperand(operandLeft));
        } else {
            registerSource = this.valueToOperand(operandLeft);
        }
        new Binary(targetBasicBlock, targetBinaryOps, destinationRegister, registerSource, this.valueToOperand(operandRight));
    }

    private void transformCastInst(CastInst<?> castInst, TargetBasicBlock targetBasicBlock) {
        if (castInst instanceof CastInst.TruncInst) {
            VirtualRegister destinationRegister = this.getOrGenVirtualRegister(castInst, targetBasicBlock.parent());
            TargetOperand castOrigin = this.valueToOperand(castInst.getSourceOperand());
            // 如果是立即数要先加载到寄存器中
            if (castOrigin instanceof Immediate) {
                VirtualRegister immediateRegister = targetBasicBlock.parent().addVirtualRegister();
                new Move(targetBasicBlock, immediateRegister, castOrigin);
                castOrigin = immediateRegister;
            }
            new Binary(targetBasicBlock, Binary.BinaryOs.AND, destinationRegister, castOrigin, Immediate.FF());
        } else if (castInst instanceof CastInst.ZExtInst) {
            VirtualRegister destinationRegister = this.getOrGenVirtualRegister(castInst, targetBasicBlock.parent());
            new Move(targetBasicBlock, destinationRegister, this.valueToOperand(castInst.getSourceOperand()));
        } else {
            IRValue<?> sourceIRValue = castInst.getSourceOperand();
            this.valueMap.put(castInst, this.valueToOperand(sourceIRValue));
        }
    }

    private void transformGetElementPtrInst(GetElementPtrInst getElementPtrInst, TargetBasicBlock targetBasicBlock) {
        IRValue<PointerType> pointerIRValue = getElementPtrInst.getPointerOperand();
        IntegerType elementIntegerType;
        IRValue<IntegerType> indexIRValue;
        if (pointerIRValue.type().referenceType() instanceof IntegerType pointIntegerType
                && getElementPtrInst.getNumIndices() == 1) {
            // 访问指针
            elementIntegerType = pointIntegerType;
            indexIRValue = getElementPtrInst.getIndexOperand(0);
        } else if (pointerIRValue.type().referenceType() instanceof ArrayType pointArrayType
                && pointArrayType.elementType() instanceof IntegerType arrayElementIntegerType
                && getElementPtrInst.getNumIndices() == 2) {
            // 访问数组
            elementIntegerType = arrayElementIntegerType;
            indexIRValue = getElementPtrInst.getIndexOperand(1);
        } else {
            throw new RuntimeException("When transformGetElementPtrInst(), the GetElementPtrInst is invalid. " +
                    "Got " + getElementPtrInst);
        }
        if (this.valueToOperand(pointerIRValue) instanceof LabelBaseAddress labelBaseAddress) {
            // Label作为基地址 全局变量
            LabelBaseAddress designatedLabelBaseAddress;
            if (this.valueToOperand(indexIRValue) instanceof Immediate immediateOffset) {
                if (IRType.isEqual(elementIntegerType, IRType.getInt8Ty())) {
                    // 全局变量 char 立即数偏移
                    designatedLabelBaseAddress = labelBaseAddress.addImmediateOffset(immediateOffset);
                } else if (IRType.isEqual(elementIntegerType, IRType.getInt32Ty())) {
                    // 全局变量 int  立即数偏移
                    designatedLabelBaseAddress = labelBaseAddress.addImmediateOffset(immediateOffset.multiplyFour());
                } else {
                    throw new RuntimeException("When transformGetElementPtrInst(), elementIntegerType is invalid. " +
                            "Got " + elementIntegerType);
                }
            } else if (this.valueToOperand(indexIRValue) instanceof TargetRegister registerOffset) {
                TargetRegister addressOffsetRegister;
                if (IRType.isEqual(elementIntegerType, IRType.getInt8Ty())) {
                    // 全局变量 char 寄存器偏移
                    addressOffsetRegister = registerOffset;
                } else if (IRType.isEqual(elementIntegerType, IRType.getInt32Ty())) {
                    // 全局变量 int  寄存器偏移
                    addressOffsetRegister = targetBasicBlock.parent().addVirtualRegister();
                    new Binary(targetBasicBlock, Binary.BinaryOs.SLL, addressOffsetRegister, registerOffset, Immediate.TWO());
                } else {
                    throw new RuntimeException("When transformGetElementPtrInst(), elementIntegerType is invalid. " +
                            "Got " + elementIntegerType);
                }
                designatedLabelBaseAddress = labelBaseAddress.addRegisterOffset(addressOffsetRegister);
            } else {
                throw new RuntimeException("When transformGetElementPtrInst(), te operand of index of GetElementPtrInst is not an Immediate or a TargetRegister");
            }
            // 登记定位的地址
            this.valueMap.put(getElementPtrInst, designatedLabelBaseAddress);
        } else if (this.valueToOperand(pointerIRValue) instanceof RegisterBaseAddress registerBaseAddress) {
            // Register作为基地址 局部变量
            RegisterBaseAddress designatedRegisterBaseAddress;
            if (this.valueToOperand(indexIRValue) instanceof Immediate immediateOffset) {
                if (IRType.isEqual(elementIntegerType, IRType.getInt8Ty())) {
                    // 局部变量 char 立即数偏移
                    designatedRegisterBaseAddress = registerBaseAddress.addImmediateOffset(immediateOffset);
                } else if (IRType.isEqual(elementIntegerType, IRType.getInt32Ty())) {
                    // 局部变量 char 立即数偏移
                    designatedRegisterBaseAddress = registerBaseAddress.addImmediateOffset(immediateOffset.multiplyFour());
                } else {
                    throw new RuntimeException("When transformGetElementPtrInst(), elementIntegerType is invalid. " +
                            "Got " + elementIntegerType);
                }
            } else if (this.valueToOperand(indexIRValue) instanceof TargetRegister registerOffset) {
                TargetRegister addressOffsetRegister;
                if (IRType.isEqual(elementIntegerType, IRType.getInt8Ty())) {
                    // 局部变量 char 寄存器偏移
                    addressOffsetRegister = registerOffset;
                } else if (IRType.isEqual(elementIntegerType, IRType.getInt32Ty())) {
                    // 局部变量 int  寄存器偏移
                    addressOffsetRegister = targetBasicBlock.parent().addVirtualRegister();
                    new Binary(targetBasicBlock, Binary.BinaryOs.SLL, addressOffsetRegister, registerOffset, Immediate.TWO());
                } else {
                    throw new RuntimeException("When transformGetElementPtrInst(), elementIntegerType is invalid. " +
                            "Got " + elementIntegerType);
                }
                // 得到新的基地址寄存器
                VirtualRegister newBaseRegister = targetBasicBlock.parent().addVirtualRegister();
                new Binary(targetBasicBlock, Binary.BinaryOs.ADD, newBaseRegister, registerBaseAddress.base(), addressOffsetRegister);
                designatedRegisterBaseAddress = registerBaseAddress.replaceBaseRegister(newBaseRegister);
            } else {
                throw new RuntimeException("When transformGetElementPtrInst(), te operand of index of GetElementPtrInst is not an Immediate or a TargetRegister");
            }
            // 登记定位的地址
            this.valueMap.put(getElementPtrInst, designatedRegisterBaseAddress);
        } else {
            throw new RuntimeException("When transformGetElementPtrInst(), the operand of pointer of GetElementPtrInst is not a TargetAddress");
        }
    }

    private void transformLoadInst(LoadInst loadInst, TargetBasicBlock targetBasicBlock) {
        IRValue<PointerType> loadPointer = loadInst.getPointerOperand();
        VirtualRegister destinationRegister = this.getOrGenVirtualRegister(loadInst, targetBasicBlock.parent());
        if (IRType.isEqual(loadPointer.type().referenceType(), IRType.getInt8Ty())) {
            new Load(targetBasicBlock, Load.SIZE.BYTE, destinationRegister, this.valueToOperand(loadPointer));
        } else if (IRType.isEqual(loadPointer.type().referenceType(), IRType.getInt32Ty())) {
            new Load(targetBasicBlock, Load.SIZE.WORD, destinationRegister, this.valueToOperand(loadPointer));
        } else if (loadPointer.type().referenceType() instanceof PointerType) {
            // 加载参数
            new Load(targetBasicBlock, Load.SIZE.WORD, destinationRegister, this.valueToOperand(loadPointer));
            RegisterBaseAddress registerBaseAddress = new RegisterBaseAddress(destinationRegister, Immediate.ZERO());
            this.valueMap.put(loadInst, registerBaseAddress);
        } else {
            throw new RuntimeException("When transformLoadInst(), LoadInst try to load a IRValue whose referenceType is invalid. Got " + loadPointer);
        }
    }

    private void transformStoreInst(StoreInst storeInst, TargetBasicBlock targetBasicBlock) {
        IRValue<PointerType> storePointer = storeInst.getPointerOperand();
        TargetOperand storeOrigin = this.valueToOperand(storeInst.getValueOperand());
        // 如果是立即数要先加载到寄存器中
        if (storeOrigin instanceof Immediate) {
            VirtualRegister immediateRegister = targetBasicBlock.parent().addVirtualRegister();
            new Move(targetBasicBlock, immediateRegister, storeOrigin);
            storeOrigin = immediateRegister;
        }
        if (IRType.isEqual(storePointer.type().referenceType(), IRType.getInt8Ty())) {
            new Store(targetBasicBlock, Store.SIZE.BYTE, storeOrigin, this.valueToOperand(storePointer));
        } else if (IRType.isEqual(storePointer.type().referenceType(), IRType.getInt32Ty())) {
            new Store(targetBasicBlock, Store.SIZE.WORD, storeOrigin, this.valueToOperand(storePointer));
        } else {
            throw new RuntimeException("When transformStoreInst(), StoreInst try to store to a IRValue whose referenceType is other than IntegerType. Got " + storePointer);
        }
    }

    private void transformCallInst(CallInst callInst, TargetBasicBlock targetBasicBlock) {
        // 有函数调用，要保存$ra
        targetBasicBlock.parent().stackFrame.ensureSaveRA();
        IRFunction calledIRFunction = callInst.getCalledFunction();
        // 处理参数
        for (int i = 0; i < callInst.getNumArgs(); i++) {
            if (i <= 3) {
                // 使用寄存器传递
                new Move(targetBasicBlock,
                        PhysicalRegister.argumentRegisterOfArgumentNumber(i),
                        this.valueToOperand(callInst.getArgOperand(i)));
            } else {
                // 使用内存传递
                RegisterBaseAddress outArgumentAddress = targetBasicBlock.parent().stackFrame.getOutArgumentAddress(i);
                TargetOperand outArgumentOperand = this.valueToOperand(callInst.getArgOperand(i));
                // 如果是立即数要先加载到寄存器中
                if (outArgumentOperand instanceof Immediate || outArgumentOperand instanceof TargetAddress<?, ?>) {
                    VirtualRegister immediateRegister = targetBasicBlock.parent().addVirtualRegister();
                    new Move(targetBasicBlock, immediateRegister, outArgumentOperand);
                    outArgumentOperand = immediateRegister;
                }
                if (IRType.isEqual(calledIRFunction.arguments().get(i).type(), IRType.getInt8Ty())) {
                    new Store(targetBasicBlock, Store.SIZE.BYTE, outArgumentOperand, outArgumentAddress);
                } else if (IRType.isEqual(calledIRFunction.arguments().get(i).type(), IRType.getInt32Ty())) {
                    new Store(targetBasicBlock, Store.SIZE.WORD, outArgumentOperand, outArgumentAddress);
                } else if (calledIRFunction.arguments().get(i).type() instanceof PointerType) {
                    new Store(targetBasicBlock, Store.SIZE.WORD, outArgumentOperand, outArgumentAddress);
                } else {
                    throw new RuntimeException("When transformCallInst, the type of argument " + i + "is invalid. " +
                            "Got " + calledIRFunction.arguments().get(i));
                }
            }
        }
        if (calledIRFunction.isLib()) {
            new Syscall(targetBasicBlock, this.syscallMap.get(calledIRFunction));
        } else {
            new Branch(targetBasicBlock, this.valueToOperand(calledIRFunction), true);
        }
        if (callInst.type() instanceof IntegerType) {
            VirtualRegister resultRegister = this.getOrGenVirtualRegister(callInst, targetBasicBlock.parent());
            // 移走结果寄存器中的值
            new Move(targetBasicBlock, resultRegister, PhysicalRegister.V0);
        } else if (!(callInst.type() instanceof VoidType)) {
            throw new RuntimeException("When transformCallInst(), the return type of CallInst is invalid. " +
                    "Got " + callInst.type());
        }
    }

    private void transformBranchInst(BranchInst branchInst, TargetBasicBlock targetBasicBlock) {
        if (branchInst.isConditional()) {
            // 有条件跳转
            if (this.valueToOperand(branchInst.getCondition()) instanceof TargetRegister condRegister) {
                TargetBasicBlock trueSuccessor = this.basicBlockBranchMap.get(targetBasicBlock).get(this.basicBlockMap.get(branchInst.getTrueSuccessor()));
                TargetBasicBlock falseSuccessor = this.basicBlockBranchMap.get(targetBasicBlock).get(this.basicBlockMap.get(branchInst.getFalseSuccessor()));
                new Branch(targetBasicBlock, condRegister, trueSuccessor.label());
                new Branch(targetBasicBlock, falseSuccessor.label(), false);
            } else {
                throw new RuntimeException("When transformBranchInst(), the operand of cond of branchInst is not a TargetRegister");
            }
        } else {
            // 无条件跳转
            TargetBasicBlock successor = this.basicBlockBranchMap.get(targetBasicBlock).get(this.basicBlockMap.get(branchInst.getSuccessor()));
            new Branch(targetBasicBlock, successor.label(), false);
        }
    }

    private void transformReturnInst(ReturnInst returnInst, TargetBasicBlock targetBasicBlock) {
        if (returnInst.getReturnValue() != null) {
            // 有参数
            // 因为之后将进入函数尾声，$v0的原始值不会再被使用了
            new Move(targetBasicBlock, PhysicalRegister.V0, this.valueToOperand(returnInst.getReturnValue()));
        }
        new Branch(targetBasicBlock, targetBasicBlock.parent().labelEpilogue(), false);
    }
}
