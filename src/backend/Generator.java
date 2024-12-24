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
import global.Config;
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
    // 存储库函数的IR的Function与系统调用号之间的对应关系，在函数调用处使用
    private final HashMap<IRFunction, Integer> syscallMap;

    // 存储一个函数内IR的BasicBlock与Target的BasicBlock之间的对应关系，在跳转指令处使用
    private final HashMap<IRBasicBlock, TargetBasicBlock> basicBlockMap;
    // 存储一个函数内跳转到Target的BasicBlock与实际要跳转到的Label之间的对应关系，在处理PHI指令时记录，在跳转指令处使用
    private final HashMap<TargetBasicBlock, HashMap<TargetBasicBlock, TargetBasicBlock>> basicBlockBranchMap;
    // 存储一个函数内Target的BasicBlock中还未释放的消除PHI指令的Move，在跳转指令前释放
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
        } else if (irValue instanceof Argument || irValue instanceof IRInstruction) {
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
            this.transformFunctionArgument(irFunction, targetFunction);
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

    private void transformFunctionArgument(IRFunction irFunction, TargetFunction targetFunction) {
        ArrayList<Argument> arguments = irFunction.arguments();
        for (int i = 0; i < arguments.size(); i++) {
            if (PhysicalRegister.argumentRegisterOfArgumentNumber(i) != null) {
                // 保存第0-3个参数对应的物理寄存器
                targetFunction.stackFrame.ensureSaveRegister(PhysicalRegister.argumentRegisterOfArgumentNumber(i));
            }
            // 为剩余参数登记对应的虚拟寄存器
            VirtualRegister newArgRegister = new VirtualRegister();
            newArgRegister.setAddress(targetFunction.stackFrame.getInArgumentAddress(i));
            if (arguments.get(i).type() instanceof IntegerType) {
                this.valueMap.put(arguments.get(i), newArgRegister);
            } else if (arguments.get(i).type() instanceof PointerType) {
                this.valueMap.put(arguments.get(i), new RegisterBaseAddress(newArgRegister, Immediate.ZERO()));
            }
        }
    }

    private void transformFunctionArgBlock(IRBasicBlock argBlock, IRFunction irFunction, TargetFunction targetFunction) {
        ArrayList<Argument> arguments = irFunction.arguments();
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
                VirtualRegister phiVirtualRegister = this.getOrGenTempVirtualRegister(phiNode, targetFunction);
                for (Pair<IRBasicBlock, IRValue<?>> blockValuePair : phiNode.getIncomingBlockValuePairs()) {
                    // 如果引用的是指令，可能要提前生成新的虚拟寄存器
                    if (blockValuePair.value() instanceof IRInstruction<?> irInstruction) {
                        this.getOrGenTempVirtualRegister(irInstruction, targetFunction);
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
            for (Pair<VirtualRegister, TargetOperand> phiCopyMove : phiCopyEntry.getValue()) {
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
                        VirtualRegister transitiveVirtualRegister = targetFunction.addTempVirtualRegister();
                        // 增加临时Move
                        phiCopiesSequential.get(phiCopyEntry.getKey()).push(new Pair<>(transitiveVirtualRegister, phiCopyMove.value()));
                        phiCopiesSequential.get(phiCopyEntry.getKey()).add(new Pair<>(phiCopyMove.key(), transitiveVirtualRegister));
                    } else {
                        // 直接采用当前Move
                        phiCopiesSequential.get(phiCopyEntry.getKey()).add(new Pair<>(phiCopyMove.key(), phiCopyMove.value()));
                    }
                }
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

    // 用于动态决定是否为临时变量分配寄存器，因为PHI是乱序的
    private VirtualRegister getOrGenTempVirtualRegister(IRInstruction<?> irInstruction, TargetFunction targetFunction) {
        if (this.valueMap.containsKey(irInstruction)) {
            // CAST 生成规范限制
            return (VirtualRegister) this.valueMap.get(irInstruction);
        } else {
            VirtualRegister virtualRegister = targetFunction.addTempVirtualRegister();
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
                //noinspection UnnecessaryContinue
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

    private void transformDivConst(VirtualRegister destinationRegister, TargetOperand registerSource,
                                   Integer divisor, TargetBasicBlock targetBasicBlock) {
        // 除-1和除1
        if (divisor == -1) {
            new Binary(targetBasicBlock, Binary.BinaryOs.SUB, destinationRegister, PhysicalRegister.ZERO, registerSource);
            return;
        } else if (divisor == 1) {
            new Move(targetBasicBlock, destinationRegister, registerSource);
            return;
        }
        // 除数取绝对值
        boolean isNegativeDivisor = divisor < 0;
        divisor = Math.abs(divisor);
        if ((divisor & (divisor - 1)) == 0) {
            // 除数是2的幂次
            int times = Integer.toBinaryString(divisor).length() - 1;
            // 由于高级语言的除法是向0取整，但是移位运算使得除法是向下取整的，所以需要处理被除数
            VirtualRegister sraRegister = targetBasicBlock.parent().addTempVirtualRegister();
            new Binary(targetBasicBlock, Binary.BinaryOs.SRA, sraRegister, registerSource, new Immediate(31));
            VirtualRegister srlRegister = targetBasicBlock.parent().addTempVirtualRegister();
            new Binary(targetBasicBlock, Binary.BinaryOs.SRL, srlRegister, sraRegister, new Immediate(32 - times));
            VirtualRegister addRegister = targetBasicBlock.parent().addTempVirtualRegister();
            new Binary(targetBasicBlock, Binary.BinaryOs.ADD, addRegister, registerSource, srlRegister);
            // 移位
            new Binary(targetBasicBlock, Binary.BinaryOs.SRA, destinationRegister, addRegister, new Immediate(times));
            // 除数是负数要取反
            if (isNegativeDivisor) {
                new Binary(targetBasicBlock, Binary.BinaryOs.SUB, destinationRegister, PhysicalRegister.ZERO, registerSource);
            }
        } else {
            // 除数不是2的幂次
            // destination = dividend / divisor
            //             = (dividend * multiplier) >> shift
            // multiplier和shift需要计算出
            // nc = 2^31 - 2^31 % divisor - 1
            long nc = (1L << 31) - ((1L << 31) % divisor) - 1;
            // 2^p > (2^31 - 2^31 % divisor - 1) * (divisor - 2^p % divisor) = nc * (divisor - 2^p % divisor)
            int p = 32;
            while ((1L << p) <= nc * (divisor - (1L << p) % divisor)) {
                p++;
            }
            // m = (2^p + divisor - 2^p % divisor) / divisor
            long m = ((1L << p) + divisor - (1L << p) % divisor) / divisor;
            // 得到multiplier和shift
            int multiplier = (int) m;
            int shift = p - 32;

            VirtualRegister multiplierRegister = targetBasicBlock.parent().addTempVirtualRegister();
            new Move(targetBasicBlock, multiplierRegister, new Immediate(multiplier));

            VirtualRegister multResultHIRegister = targetBasicBlock.parent().addTempVirtualRegister();
            if (m >= 0x80000000L) {
                // multResultHI = dividend + (dividend * multiplier)[63:32]
                new Binary(targetBasicBlock, Binary.BinaryOs.HIMADD, multResultHIRegister, registerSource, multiplierRegister);
            } else {
                // multResultHI = (dividend * multiplier)[63:32]
                new Binary(targetBasicBlock, Binary.BinaryOs.HIMULT, multResultHIRegister, registerSource, multiplierRegister);
            }

            // multResultHIShift = multResultHI >> shift
            VirtualRegister multResultHIShiftRegister = targetBasicBlock.parent().addTempVirtualRegister();
            new Binary(targetBasicBlock, Binary.BinaryOs.SRA, multResultHIShiftRegister, multResultHIRegister, new Immediate(shift));

            // dividendShift = dividend >> 31
            VirtualRegister dividendShiftRegister = targetBasicBlock.parent().addTempVirtualRegister();
            new Binary(targetBasicBlock, Binary.BinaryOs.SRA, dividendShiftRegister, registerSource, new Immediate(31));

            if (isNegativeDivisor) {
                // destination = dividendShift - multResultHIShift
                new Binary(targetBasicBlock, Binary.BinaryOs.SUB, destinationRegister, dividendShiftRegister, multResultHIShiftRegister);
            } else {
                // destination = multResultHIShift - dividendShift
                new Binary(targetBasicBlock, Binary.BinaryOs.SUB, destinationRegister, multResultHIShiftRegister, dividendShiftRegister);
            }
        }
    }

    private void transformBinaryOperator(BinaryOperator binaryOperator, TargetBasicBlock targetBasicBlock) {
        IRValue<IntegerType> operandLeft = binaryOperator.getOperand1();
        IRValue<IntegerType> operandRight = binaryOperator.getOperand2();
        VirtualRegister destinationRegister = this.getOrGenTempVirtualRegister(binaryOperator, targetBasicBlock.parent());
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
            registerSource = targetBasicBlock.parent().addTempVirtualRegister();
            new Move(targetBasicBlock, registerSource, this.valueToOperand(operandLeft));
        } else {
            registerSource = this.valueToOperand(operandLeft);
        }
        if (Config.enableBackendOptimization &&
                targetBinaryOps == Binary.BinaryOs.DIV && operandRight instanceof ConstantInt rightConstant) {
            this.transformDivConst(destinationRegister, registerSource, rightConstant.constantValue(), targetBasicBlock);
        } else {
            new Binary(targetBasicBlock, targetBinaryOps, destinationRegister, registerSource, this.valueToOperand(operandRight));
        }
    }

    private void transformIcmpInst(IcmpInst icmpInst, TargetBasicBlock targetBasicBlock) {
        IRValue<IntegerType> operandLeft = icmpInst.getOperand1();
        IRValue<IntegerType> operandRight = icmpInst.getOperand2();
        VirtualRegister destinationRegister = this.getOrGenTempVirtualRegister(icmpInst, targetBasicBlock.parent());
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
            registerSource = targetBasicBlock.parent().addTempVirtualRegister();
            new Move(targetBasicBlock, registerSource, this.valueToOperand(operandLeft));
        } else {
            registerSource = this.valueToOperand(operandLeft);
        }
        new Binary(targetBasicBlock, targetBinaryOps, destinationRegister, registerSource, this.valueToOperand(operandRight));
    }

    private void transformCastInst(CastInst<?> castInst, TargetBasicBlock targetBasicBlock) {
        if (castInst instanceof CastInst.TruncInst) {
            VirtualRegister destinationRegister = this.getOrGenTempVirtualRegister(castInst, targetBasicBlock.parent());
            TargetOperand castOrigin = this.valueToOperand(castInst.getSourceOperand());
            // 如果是立即数要先加载到寄存器中
            if (castOrigin instanceof Immediate) {
                VirtualRegister immediateRegister = targetBasicBlock.parent().addTempVirtualRegister();
                new Move(targetBasicBlock, immediateRegister, castOrigin);
                castOrigin = immediateRegister;
            }
            new Binary(targetBasicBlock, Binary.BinaryOs.AND, destinationRegister, castOrigin, Immediate.FF());
        } else if (castInst instanceof CastInst.ZExtInst) {
            VirtualRegister destinationRegister = this.getOrGenTempVirtualRegister(castInst, targetBasicBlock.parent());
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
        if (this.valueToOperand(pointerIRValue) instanceof TargetAddress<?, ?> pointerTargetAddress) {
            TargetAddress<?, ?> designatedTargetAddress;
            if (this.valueToOperand(indexIRValue) instanceof Immediate immediateOffset) {
                if (IRType.isEqual(elementIntegerType, IRType.getInt8Ty())) {
                    // char 立即数偏移
                    designatedTargetAddress = pointerTargetAddress.addImmediateOffset(immediateOffset);
                } else if (IRType.isEqual(elementIntegerType, IRType.getInt32Ty())) {
                    // int  立即数偏移
                    designatedTargetAddress = pointerTargetAddress.addImmediateOffset(immediateOffset.multiplyFour());
                } else {
                    throw new RuntimeException("When transformGetElementPtrInst(), elementIntegerType is invalid. " +
                            "Got " + elementIntegerType);
                }
            } else if (this.valueToOperand(indexIRValue) instanceof TargetRegister registerOffset) {
                if (pointerTargetAddress instanceof LabelBaseAddress labelBaseAddress) {
                    TargetRegister addressOffsetRegister;
                    if (IRType.isEqual(elementIntegerType, IRType.getInt8Ty())) {
                        // 全局变量 char 寄存器偏移
                        addressOffsetRegister = registerOffset;
                    } else if (IRType.isEqual(elementIntegerType, IRType.getInt32Ty())) {
                        // 全局变量 int  寄存器偏移
                        addressOffsetRegister = targetBasicBlock.parent().addTempVirtualRegister();
                        new Binary(targetBasicBlock, Binary.BinaryOs.SLL, addressOffsetRegister, registerOffset, Immediate.TWO());
                    } else {
                        throw new RuntimeException("When transformGetElementPtrInst(), elementIntegerType is invalid. " +
                                "Got " + elementIntegerType);
                    }
                    designatedTargetAddress = labelBaseAddress.addRegisterOffset(addressOffsetRegister);
                } else if (this.valueToOperand(pointerIRValue) instanceof RegisterBaseAddress registerBaseAddress) {
                    TargetRegister addressOffsetRegister;
                    if (IRType.isEqual(elementIntegerType, IRType.getInt8Ty())) {
                        // 局部变量 char 寄存器偏移
                        addressOffsetRegister = registerOffset;
                    } else if (IRType.isEqual(elementIntegerType, IRType.getInt32Ty())) {
                        // 局部变量 int  寄存器偏移
                        addressOffsetRegister = targetBasicBlock.parent().addTempVirtualRegister();
                        new Binary(targetBasicBlock, Binary.BinaryOs.SLL, addressOffsetRegister, registerOffset, Immediate.TWO());
                    } else {
                        throw new RuntimeException("When transformGetElementPtrInst(), elementIntegerType is invalid. " +
                                "Got " + elementIntegerType);
                    }
                    // 得到新的基地址寄存器
                    VirtualRegister newBaseRegister = targetBasicBlock.parent().addTempVirtualRegister();
                    new Binary(targetBasicBlock, Binary.BinaryOs.ADD, newBaseRegister, registerBaseAddress.base(), addressOffsetRegister);
                    designatedTargetAddress = registerBaseAddress.replaceBaseRegister(newBaseRegister);
                } else {
                    throw new RuntimeException("When transformGetElementPtrInst(), the operand of pointer of GetElementPtrInst is not a implemented TargetAddress");
                }
            } else {
                throw new RuntimeException("When transformGetElementPtrInst(), te operand of index of GetElementPtrInst is not an Immediate or a TargetRegister");
            }
            // 登记定位的地址
            this.valueMap.put(getElementPtrInst, designatedTargetAddress);
        } else {
            throw new RuntimeException("When transformGetElementPtrInst(), the operand of pointer of GetElementPtrInst is not a TargetAddress");
        }
    }

    private void transformLoadInst(LoadInst loadInst, TargetBasicBlock targetBasicBlock) {
        IRValue<PointerType> loadPointer = loadInst.getPointerOperand();
        VirtualRegister destinationRegister = this.getOrGenTempVirtualRegister(loadInst, targetBasicBlock.parent());
        if (IRType.isEqual(loadPointer.type().referenceType(), IRType.getInt8Ty())) {
            new Load(targetBasicBlock, Load.SIZE.BYTE, destinationRegister, this.valueToOperand(loadPointer));
        } else if (IRType.isEqual(loadPointer.type().referenceType(), IRType.getInt32Ty())) {
            new Load(targetBasicBlock, Load.SIZE.WORD, destinationRegister, this.valueToOperand(loadPointer));
        } else if (loadPointer.type().referenceType() instanceof PointerType) {
            // 加载参数
            new Load(targetBasicBlock, Load.SIZE.WORD, destinationRegister, this.valueToOperand(loadPointer));
            RegisterBaseAddress registerBaseAddress = new RegisterBaseAddress(destinationRegister, Immediate.ZERO());
            // 登记定位的地址
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
            VirtualRegister immediateRegister = targetBasicBlock.parent().addTempVirtualRegister();
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
            if (PhysicalRegister.argumentRegisterOfArgumentNumber(i) != null) {
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
                    VirtualRegister immediateRegister = targetBasicBlock.parent().addTempVirtualRegister();
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
            VirtualRegister resultRegister = this.getOrGenTempVirtualRegister(callInst, targetBasicBlock.parent());
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
