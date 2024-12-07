package backend;

import IR.IRModule;
import IR.type.*;
import IR.value.*;
import IR.value.constant.*;
import IR.value.instruction.*;
import backend.target.TargetBasicBlock;
import backend.target.TargetDataObject;
import backend.target.TargetFunction;
import backend.target.TargetModule;

import java.util.ArrayList;
import java.util.HashMap;

public class Generator {
    private final IRModule irModule;
    private boolean finish = false;

    // 存储IR的Function与Target的Function之间的对应关系，在函数调用时使用
    private final HashMap<IRFunction, TargetFunction> functionMap;
    // 存储一个函数内IR的BasicBlock与Target的BasicBlock之间的对应关系，在跳转指令出使用
    private final HashMap<IRBasicBlock, TargetBasicBlock> basicBlockMap;

    private final TargetModule targetModule;

    public Generator(IRModule irModule) {
        this.irModule = irModule;
        this.targetModule = new TargetModule();
        this.functionMap = new HashMap<>();
        this.basicBlockMap = new HashMap<>();
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

    private void transformIRGlobalVariable(IRGlobalVariable irGlobalVariable) {
        TargetDataObject targetDataObject = new TargetDataObject(irGlobalVariable.name());
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
        // BasicBlock的对应关系局限于一个函数，新的一个函数需要清空
        this.basicBlockMap.clear();
        if (irFunction.basicBlocks().size() < 3) {
            throw new RuntimeException("When transformIRFunction(), the basic block of function " + irFunction.name() +
                    " is less than requirements.");
        }
        this.transformFunctionArgBlock(irFunction.basicBlocks().get(0), targetFunction);
        this.transformFunctionDefBlock(irFunction.basicBlocks().get(1), targetFunction);
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
    }

    private void transformFunctionDefBlock(IRBasicBlock defBlock, TargetFunction targetFunction) {

    }

    private void transformIRBasicBlock(IRBasicBlock irBasicBlock,
                                       TargetBasicBlock targetBasicBlock, TargetFunction targetFunction) {

    }

    private void transformIRInstruction(IRInstruction<?> instruction,
                                        TargetBasicBlock targetBasicBlock, TargetFunction targetFunction) {

    }
}
