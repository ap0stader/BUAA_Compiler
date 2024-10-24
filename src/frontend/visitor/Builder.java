package frontend.visitor;

import IR.*;
import IR.type.*;
import IR.value.*;
import IR.value.constant.*;
import IR.value.instruction.*;
import frontend.visitor.symbol.ConstSymbol;
import frontend.visitor.symbol.VarSymbol;
import util.Pair;

import java.util.ArrayList;

class Builder {
    private final IRModule irModule;

    Builder(IRModule irModule) {
        this.irModule = irModule;
    }

    GlobalVariable addGlobalConstant(ConstSymbol constSymbol) {
        GlobalVariable globalConstant;
        if (constSymbol.type() instanceof IntegerType) {
            globalConstant = new GlobalVariable(constSymbol.type(), constSymbol.name(),
                    true, false,
                    new ConstantInt((IntegerType) constSymbol.type(), constSymbol.initVals().get(0)));
        } else if (constSymbol.type() instanceof ArrayType) {
            Pair<IRType, Constant> optimizedArray = optimizeArray((ArrayType) constSymbol.type(), constSymbol.initVals());
            globalConstant = new GlobalVariable(optimizedArray.key(), constSymbol.name(),
                    true, false,
                    optimizedArray.value());
        } else {
            throw new UnsupportedOperationException("When addGlobalConstant(), illegal type. Got " + constSymbol.type() +
                    ", expected IntegerType or ArrayType");
        }
        irModule.appendGlobalVariables(globalConstant);
        return globalConstant;
    }

    GlobalVariable addGlobalVariable(VarSymbol varSymbol, ArrayList<Integer> initVals) {
        GlobalVariable globalConstant;
        if (varSymbol.type() instanceof IntegerType) {
            globalConstant = new GlobalVariable(varSymbol.type(), varSymbol.name(),
                    false, false,
                    new ConstantInt((IntegerType) varSymbol.type(), initVals.get(0)));
        } else if (varSymbol.type() instanceof ArrayType) {
            Pair<IRType, Constant> optimizedArray = optimizeArray((ArrayType) varSymbol.type(), initVals);
            globalConstant = new GlobalVariable(optimizedArray.key(), varSymbol.name(),
                    false, false,
                    optimizedArray.value());
        } else {
            throw new UnsupportedOperationException("When addGlobalVariable(), illegal type. Got " + varSymbol.type() +
                    ", expected IntegerType or ArrayType");
        }
        irModule.appendGlobalVariables(globalConstant);
        return globalConstant;
    }

    private static Pair<IRType, Constant> optimizeArray(ArrayType originType, ArrayList<Integer> originInitVals) {
        // 寻找最后一个不是0的数字
        int lastNotZero = originInitVals.size() - 1;
        while (lastNotZero >= 0 && originInitVals.get(lastNotZero) == 0) {
            lastNotZero--;
        }
        if (lastNotZero == -1) {
            // 如果全是0，那么直接使用zeroinitializer
            return new Pair<>(originType, new ConstantAggregateZero(originType));
        } else if (originInitVals.size() - 1 - lastNotZero < 10) {
            // 如果结尾非0的数据不足10个，那么保留原样
            return new Pair<>(originType,
                    new ConstantArray(originType, convertIntegerArrayInitVal(originType, originInitVals, -1)));
        } else {
            // 如果结尾有较多的0，转换为结构体使用
            ArrayList<IRType> structMemberTypes = new ArrayList<>();
            // 有数据的部分
            for (int i = 0; i <= lastNotZero; i++) {
                structMemberTypes.add(originType.elementType());
            }
            // 均为0的部分
            ArrayType zeroPartType = new ArrayType(originType.elementType(), originInitVals.size() - 1 - lastNotZero);
            structMemberTypes.add(zeroPartType);
            // 结构体类型
            StructType structType = new StructType(structMemberTypes);
            // 有数据的部分的数据
            ArrayList<Constant> constantValues = convertIntegerArrayInitVal(originType, originInitVals, lastNotZero);
            // 均为0的部分使用zeroinitializer
            constantValues.add(new ConstantAggregateZero(zeroPartType));
            return new Pair<>(structType, new ConstantStruct(structType, constantValues));
        }
    }

    private static ArrayList<Constant> convertIntegerArrayInitVal(ArrayType type, ArrayList<Integer> initVals, int endIndex) {
        ArrayList<Constant> constantValues = new ArrayList<>();
        if (endIndex == -1) {
            endIndex = initVals.size() - 1;
        } else if (endIndex >= initVals.size()) {
            throw new IndexOutOfBoundsException("When convertIntegerArrayInitVal(), endIndex is greater than the size of initVals. " +
                    "Got " + endIndex + ", expected " + initVals.size());
        }
        for (int i = 0; i <= endIndex; i++) {
            // 由于BType只有int和char，此处强制转换不会出错
            constantValues.add(new ConstantInt((IntegerType) type.elementType(), initVals.get(i)));
        }
        return constantValues;
    }
}
