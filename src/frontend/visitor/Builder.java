package frontend.visitor;

import IR.*;
import IR.type.*;
import IR.value.*;
import IR.value.constant.*;
import IR.value.instruction.*;
import frontend.visitor.symbol.*;
import util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

class Builder {
    private final IRModule irModule;

    private final HashMap<String, Function> libFunctions;
    // 当前访问的函数
    private Function nowFunction = null;

    Builder(IRModule irModule) {
        this.irModule = irModule;
        this.libFunctions = getLibFunctions();
        getLibFunctions().forEach((name, function) -> irModule.appendFunctions(function));
    }

    private static HashMap<String, Function> getLibFunctions() {
        HashMap<String, Function> libFunctions = new HashMap<>();
        // 库函数均为声明，无需给出Argument
        ArrayList<IRType> parameters;
        // declare i32 @getint()      读取一个整数
        libFunctions.put("getint", new Function("getint", new FunctionType(IRType.getInt32Ty(), new ArrayList<>()), new ArrayList<>(), true));
        // declare i32 @getchar()     读取一个字符
        libFunctions.put("getchar", new Function("getchar", new FunctionType(IRType.getInt32Ty(), new ArrayList<>()), new ArrayList<>(), true));
        // declare void @putint(i32)  输出一个整数
        parameters = new ArrayList<>(Collections.singletonList(IRType.getInt32Ty()));
        libFunctions.put("putint", new Function("putint", new FunctionType(IRType.getVoidTy(), parameters), new ArrayList<>(), true));
        // declare void @putch(i32)   输出一个字符
        parameters = new ArrayList<>(Collections.singletonList(IRType.getInt32Ty()));
        libFunctions.put("putch", new Function("putch", new FunctionType(IRType.getVoidTy(), parameters), new ArrayList<>(), true));
        // declare void @putstr(i8*)  输出字符串
        parameters = new ArrayList<>(Collections.singletonList(new PointerType(IRType.getInt8Ty(), false)));
        libFunctions.put("putstr", new Function("putstr", new FunctionType(IRType.getVoidTy(), parameters), new ArrayList<>(), true));
        // declare void @llvm.memset.p0i8.i64(i8*, i8, i64, i1)
        parameters = new ArrayList<>(Arrays.asList(new PointerType(IRType.getInt8Ty(), false), IRType.getInt8Ty(), new IntegerType(64), new IntegerType(1)));
        libFunctions.put("memset", new Function("llvm.memset.p0i8.i64", new FunctionType(IRType.getVoidTy(), parameters), new ArrayList<>(), true));
        return libFunctions;
    }

    GlobalVariable addGlobalConstant(ConstSymbol constSymbol) {
        GlobalVariable globalConstant;
        if (constSymbol.type() instanceof IntegerType) {
            globalConstant = new GlobalVariable(constSymbol.name(), constSymbol.type(),
                    true, false,
                    new ConstantInt((IntegerType) constSymbol.type(), constSymbol.initVals().get(0)));
        } else if (constSymbol.type() instanceof ArrayType) {
            Pair<IRType, Constant> optimizedArray = optimizeArray((ArrayType) constSymbol.type(), constSymbol.initVals());
            globalConstant = new GlobalVariable(constSymbol.name(), optimizedArray.key(),
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
            globalConstant = new GlobalVariable(varSymbol.name(), varSymbol.type(),
                    false, false,
                    new ConstantInt((IntegerType) varSymbol.type(), initVals.get(0)));
        } else if (varSymbol.type() instanceof ArrayType) {
            Pair<IRType, Constant> optimizedArray = optimizeArray((ArrayType) varSymbol.type(), initVals);
            globalConstant = new GlobalVariable(varSymbol.name(), optimizedArray.key(),
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

    Function addFunction(FuncSymbol funcSymbol, ArrayList<VarSymbol> parameters) {
        Function function = new Function(funcSymbol.name(), funcSymbol.type(),
                new ArrayList<>(parameters.stream().map((parameter) -> new Argument(parameter.type())).toList()), false);
        irModule.appendFunctions(function);
        this.nowFunction = function;
        return function;
    }

    void addMainFunction() {
        // 主函数返回值为0，没有参数
        Function mainFunction = new Function("main", new FunctionType(IRType.getInt32Ty(), new ArrayList<>()),
                new ArrayList<>(), false);
        irModule.appendFunctions(mainFunction);
        this.nowFunction = mainFunction;
    }

    BasicBlock newBasicBlock() {
        BasicBlock basicBlock = new BasicBlock(this.nowFunction);
        this.nowFunction.appendBasicBlock(basicBlock);
        return basicBlock;
    }
}
