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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class Generator {
    private final IRModule irModule;
    private boolean finish = false;

    // 存储IR的GlobalVariable与Target的Address之间的对应关系，在加载全局变量时使用
    private final HashMap<IRGlobalVariable, LabelBaseAddress> globalVariableMap;
    // 存储IR的Function与Target的Function之间的对应关系，在函数调用处使用
    private final HashMap<IRFunction, TargetFunction> functionMap;
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
            targetDataObject.appendZero(initZeroArrayElementType, initZeroArrayType.length());
        } else if (irGlobalVariable.initVals() instanceof ConstantStruct initStruct) {
            // 由于StructType的IRGlobalVariable只会因为长数组的优化产生，故按照长数组优化形成的结构解析
            for (IRConstant<?> initValue : initStruct.constantValues()) {
                if (initValue instanceof ConstantInt initInt) {
                    targetDataObject.appendData(initInt);
                } else if (initValue instanceof ConstantAggregateZero initAggregateZero
                        && initAggregateZero.type() instanceof ArrayType initZeroArrayType
                        && initZeroArrayType.elementType() instanceof IntegerType initZeroArrayElementType) {
                    targetDataObject.appendZero(initZeroArrayElementType, initZeroArrayType.length());
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
            return;
        }
        TargetFunction targetFunction = new TargetFunction(irFunction.name());
        this.functionMap.put(irFunction, targetFunction);
        // BasicBlock的对应关系和Value的对应关系局限于一个函数，新的一个函数需要清空
        this.basicBlockMap.clear();
        this.valueMap.clear();
        if (irFunction.basicBlocks().size() < 3) {
            throw new RuntimeException("When transformIRFunction(), the basic block of function " + irFunction.name() +
                    " is less than requirements.");
        }
        this.transformFunctionArgBlock(irFunction.basicBlocks().get(0), targetFunction);
        this.transformFunctionDefBlock(irFunction.basicBlocks().get(1), targetFunction);
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
            this.transformIRBasicBlock(irBasicBlock, targetBasicBlock, targetFunction);
        }
        this.targetModule.appendFunctions(targetFunction);
    }

    private void transformFunctionArgBlock(IRBasicBlock argBlock, TargetFunction targetFunction) {
        ArrayList<Argument> arguments = argBlock.parent().arguments();
        // 为第0-3个参数登记对应的寄存器
        for (int i = 0; i < arguments.size(); i++) {
            if (PhysicalRegister.argumentRegisterOfArgumentNumber(i) != null) {
                this.valueMap.put(arguments.get(i), PhysicalRegister.argumentRegisterOfArgumentNumber(i));
            }
        }
        LinkedList<IRInstruction<?>> instructions = argBlock.instructions();
        // argBlock的规范是 一条alloca指令配上一条将参数写入alloca地址
        Iterator<IRInstruction<?>> argBlockIterator = instructions.iterator();
        while (argBlockIterator.hasNext()) {
            IRInstruction<?> irInstructionFirst = argBlockIterator.next();
            if (irInstructionFirst instanceof AllocaInst allocaInst && argBlockIterator.hasNext()) {
                IRInstruction<?> irInstructionSecond = argBlockIterator.next();
                if (irInstructionSecond instanceof StoreInst storeInst &&
                        // storeInst与allocaInst是搭配的
                        storeInst.getOperand(0) instanceof Argument argument &&
                        storeInst.getOperand(1) == allocaInst) {
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
            throw new RuntimeException("When transformFunctionArgBlock(), the argBlock of function " + argBlock.parent().name() + " is invalid");
        }
    }

    private void transformFunctionDefBlock(IRBasicBlock defBlock, TargetFunction targetFunction) {
        LinkedList<IRInstruction<?>> instructions = defBlock.instructions();
        // defBlock的规范是 均为alloca指令
        Iterator<IRInstruction<?>> defBlockIterator = instructions.iterator();
        while (defBlockIterator.hasNext()) {
            IRInstruction<?> irInstruction = defBlockIterator.next();
            if (irInstruction instanceof AllocaInst allocaInst) {
                if (allocaInst.allocType() instanceof IntegerType allocIntegerType &&
                        (allocIntegerType.size() == 8 || allocIntegerType.size() == 32)) {
                    this.valueMap.put(allocaInst, targetFunction.stackFrame.alloc(
                            allocIntegerType.size() / 8));
                    continue;
                } else if (allocaInst.allocType() instanceof ArrayType allocArrayType &&
                        allocArrayType.elementType() instanceof IntegerType allocArrayElementType &&
                        (allocArrayElementType.size() == 8 || allocArrayElementType.size() == 32)) {
                    this.valueMap.put(allocaInst, targetFunction.stackFrame.alloc(
                            allocArrayType.length() * (allocArrayElementType.size() / 8)));
                    continue;
                } else {
                    throw new RuntimeException("When transformFunctionDefBlock(), the allocaInst of allocaInst " + allocaInst + " is invalid. " +
                            "Got " + allocaInst.allocType());
                }
            } else if (irInstruction instanceof BranchInst && !defBlockIterator.hasNext()) {
                // defBlock正确结束
                break;
            }
            // defBlock不符合约定
            throw new RuntimeException("When transformFunctionDefBlock(), the defBlock of function " + defBlock.parent().name() + " is invalid");
        }
    }

    private void transformIRBasicBlock(IRBasicBlock irBasicBlock,
                                       TargetBasicBlock targetBasicBlock, TargetFunction targetFunction) {
        LinkedList<IRInstruction<?>> instructions = irBasicBlock.instructions();
        Iterator<IRInstruction<?>> irBasicBlockIterator = instructions.iterator();
        while (irBasicBlockIterator.hasNext()) {
            IRInstruction<?> irInstruction = irBasicBlockIterator.next();
            if (irInstruction instanceof LoadInst loadInst) {
                this.transformLoadInst(loadInst, targetBasicBlock);
            } else if (irInstruction instanceof ReturnInst returnInst) {
                this.transformReturnInst(returnInst, targetBasicBlock);
            } else {
                throw new RuntimeException("When transformIRBasicBlock(), the type of irInstruction is unsupported in a normal basic block");
            }
        }
    }

    private void transformLoadInst(LoadInst loadInst, TargetBasicBlock targetBasicBlock) {
        // CAST LoadInst的构造函数限制保证合理
        IRValue<PointerType> loadPointerIRValue = IRValue.cast(loadInst.getOperand(0));
        IntegerType loadIntegerType;
        if ((loadPointerIRValue instanceof IRGlobalVariable irGlobalVariable
                && irGlobalVariable.variableType() instanceof IntegerType integerType)) {
            loadIntegerType = integerType;
        } else if (loadPointerIRValue instanceof AllocaInst allocaInst &&
                allocaInst.allocType() instanceof IntegerType integerType) {
            loadIntegerType = integerType;
        } else {
            throw new RuntimeException("When transformLoadInst(), LoadInst try to load a IRValue whose referenceType is other than IntegerType. Got " + loadPointerIRValue);
        }
        VirtualRegister destinationRegister = targetBasicBlock.parent().addVirtualRegister();
        new Load(targetBasicBlock, loadIntegerType.size(), destinationRegister, this.valueToOperand(loadPointerIRValue));
        this.valueMap.put(loadInst, destinationRegister);
    }

    private void transformReturnInst(ReturnInst returnInst, TargetBasicBlock targetBasicBlock) {
        if (returnInst.getNumOperands() == 1) {
            // 有参数
            // 因为之后将进入函数尾声，$v0的原始值不会再被使用了
            new Move(targetBasicBlock, PhysicalRegister.V0, this.valueToOperand(returnInst.getOperand(0)));
        }
        new Branch(targetBasicBlock, targetBasicBlock.parent().labelEpilogue());
    }
}
