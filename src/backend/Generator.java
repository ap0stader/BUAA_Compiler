package backend;

import IR.IRModule;
import IR.type.*;
import IR.value.*;
import IR.value.constant.*;
import IR.value.instruction.*;
import backend.target.TargetDataObject;
import backend.target.TargetModule;

public class Generator {
    private final IRModule irModule;
    private boolean finish = false;

    private final TargetModule targetModule;

    public Generator(IRModule irModule) {
        this.irModule = irModule;
        this.targetModule = new TargetModule();
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

    }
}
