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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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
            // BasicBlock的对应关系和Value的对应关系局限于一个函数，新的一个函数需要清空
            this.basicBlockMap.clear();
            this.valueMap.clear();
            if (irFunction.basicBlocks().size() < 3) {
                throw new RuntimeException("When transformIRFunction(), the basic block of function " + irFunction.name() +
                        " is less than requirements.");
            }
            this.transformFunctionArgBlock(irFunction.basicBlocks().get(0), irFunction, targetFunction);
            this.transformFunctionDefBlock(irFunction.basicBlocks().get(1), irFunction, targetFunction);
            // 先创建好一个函数的所有的TargetBasicBlock，方便后续跳转时使用Label
            for (int i = 2; i < irFunction.basicBlocks().size(); i++) {
                IRBasicBlock irBasicBlock = irFunction.basicBlocks().get(i);
                TargetBasicBlock targetBasicBlock = new TargetBasicBlock(targetFunction, i - 2);
                this.basicBlockMap.put(irBasicBlock, targetBasicBlock);
                targetFunction.appendBasicBlock(targetBasicBlock);
            }
            for (int i = 2; i < irFunction.basicBlocks().size(); i++) {
                IRBasicBlock irBasicBlock = irFunction.basicBlocks().get(i);
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
                    // 如果是通过寄存器传递的参数仍然在argBlock中保存了，那么要将寄存器保存到栈上合理的位置
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
                this.transformBranchInst(branchInst, targetBasicBlock);
            } else if (irInstruction instanceof ReturnInst returnInst) {
                this.transformReturnInst(returnInst, targetBasicBlock);
            } else {
                throw new RuntimeException("When transformIRBasicBlock(), the type of irInstruction is unsupported in a normal basic block");
            }
        }
    }

    private void transformBinaryOperator(BinaryOperator binaryOperator, TargetBasicBlock targetBasicBlock) {
        IRValue<IntegerType> operandLeft = binaryOperator.getOperand1();
        IRValue<IntegerType> operandRight = binaryOperator.getOperand2();
        VirtualRegister destinationRegister = targetBasicBlock.parent().addVirtualRegister();
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
        new Binary(targetBasicBlock, targetBinaryOps, destinationRegister, registerSource, this.valueToOperand(operandRight));
        this.valueMap.put(binaryOperator, destinationRegister);
    }

    private void transformIcmpInst(IcmpInst icmpInst, TargetBasicBlock targetBasicBlock) {
        IRValue<IntegerType> operandLeft = icmpInst.getOperand1();
        IRValue<IntegerType> operandRight = icmpInst.getOperand2();
        VirtualRegister destinationRegister = targetBasicBlock.parent().addVirtualRegister();
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
        this.valueMap.put(icmpInst, destinationRegister);
    }

    @SuppressWarnings("unused")
    private void transformCastInst(CastInst<?> castInst, TargetBasicBlock targetBasicBlock) {
        // MAYBE
        // 对于char类型
        // - 存储 采用lbu和sb，只取最低的byte
        // - 计算 整型提升至32位
        // - 输出 syscall 11，只取最低的byte
        // 对于bool类型
        // - 来源 均为IcmpInst产生，对应s__
        // - 计算 整型提升至32位
        // 故对于Zext和Trunc不需要特别处理
        IRValue<?> sourceIRValue = castInst.getSourceOperand();
        this.valueMap.put(castInst, this.valueToOperand(sourceIRValue));
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
        VirtualRegister destinationRegister = targetBasicBlock.parent().addVirtualRegister();
        if (IRType.isEqual(loadPointer.type().referenceType(), IRType.getInt8Ty())) {
            new Load(targetBasicBlock, Load.SIZE.BYTE, destinationRegister, this.valueToOperand(loadPointer));
            this.valueMap.put(loadInst, destinationRegister);
        } else if (IRType.isEqual(loadPointer.type().referenceType(), IRType.getInt32Ty())) {
            new Load(targetBasicBlock, Load.SIZE.WORD, destinationRegister, this.valueToOperand(loadPointer));
            this.valueMap.put(loadInst, destinationRegister);
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
            VirtualRegister resultRegister = targetBasicBlock.parent().addVirtualRegister();
            new Move(targetBasicBlock, resultRegister, PhysicalRegister.V0);
            this.valueMap.put(callInst, resultRegister);
        } else if (!(callInst.type() instanceof VoidType)) {
            throw new RuntimeException("When transformCallInst(), the return type of CallInst is invalid. " +
                    "Got " + callInst.type());
        }
    }

    private void transformBranchInst(BranchInst branchInst, TargetBasicBlock targetBasicBlock) {
        if (branchInst.isConditional()) {
            // 有条件跳转
            if (this.valueToOperand(branchInst.getCondition()) instanceof TargetRegister condRegister) {
                new Branch(targetBasicBlock, condRegister, this.valueToOperand(branchInst.getTrueSuccessor()));
                new Branch(targetBasicBlock, this.valueToOperand(branchInst.getFalseSuccessor()), false);
            } else {
                throw new RuntimeException("When transformBranchInst(), the operand of cond of branchInst is not a TargetRegister");
            }
        } else {
            // 无条件跳转
            new Branch(targetBasicBlock, this.valueToOperand(branchInst.getSuccessor()), false);
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
