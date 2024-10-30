package frontend.visitor;

import IR.*;
import IR.type.*;
import IR.value.*;
import IR.value.constant.*;
import IR.value.instruction.*;
import frontend.lexer.Token;
import frontend.visitor.symbol.*;
import global.Config;
import util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

class Builder {
    private final IRModule irModule;

    // 库函数
    private final HashMap<String, Function> libFunctions;
    // 静态字符串
    private final HashMap<String, GlobalVariable> constStr;
    // 当前访问的函数
    private Function nowFunction = null;

    Builder(IRModule irModule) {
        this.irModule = irModule;
        this.libFunctions = getLibFunctions();
        this.constStr = new HashMap<>();
        // 将库函数的定义添加到IRModule中
        getLibFunctions().forEach((name, function) -> irModule.appendFunctions(function));
    }

    private static HashMap<String, Function> getLibFunctions() {
        HashMap<String, Function> libFunctions = new HashMap<>();
        // 库函数均为声明，无需给出Argument
        ArrayList<IRType> parameters;
        // declare i32 @getint()      读取一个整数
        libFunctions.put("getint", new Function("getint", new FunctionType(IRType.getInt32Ty(), new ArrayList<>())));
        // declare i32 @getchar()     读取一个字符
        libFunctions.put("getchar", new Function("getchar", new FunctionType(IRType.getInt32Ty(), new ArrayList<>())));
        // declare void @putint(i32)  输出一个整数
        parameters = new ArrayList<>(Collections.singletonList(IRType.getInt32Ty()));
        libFunctions.put("putint", new Function("putint", new FunctionType(IRType.getVoidTy(), parameters)));
        // declare void @putch(i32)   输出一个字符
        parameters = new ArrayList<>(Collections.singletonList(IRType.getInt32Ty()));
        libFunctions.put("putch", new Function("putch", new FunctionType(IRType.getVoidTy(), parameters)));
        // declare void @putstr(i8*)  输出字符串
        parameters = new ArrayList<>(Collections.singletonList(new PointerType(IRType.getInt8Ty(), false)));
        libFunctions.put("putstr", new Function("putstr", new FunctionType(IRType.getVoidTy(), parameters)));
        // TODO 对于长数组进行优化
        // declare void @llvm.memset.p0i8.i64(i8*, i8, i64, i1)
        // parameters = new ArrayList<>(Arrays.asList(new PointerType(IRType.getInt8Ty(), false), IRType.getInt8Ty(), IRType.getInt64Ty(), IRType.getInt1Ty()));
        // libFunctions.put("memset", new Function("llvm.memset.p0i8.i64", new FunctionType(IRType.getVoidTy(), parameters)));
        return libFunctions;
    }

    GlobalVariable addGlobalConstant(ConstSymbol constSymbol) {
        GlobalVariable globalConstant;
        if (constSymbol.type() instanceof IntegerType constSymbolType) {
            globalConstant = new GlobalVariable(constSymbol.name(), constSymbol.type(),
                    true, false,
                    new ConstantInt(constSymbolType, constSymbol.initVals().get(0)));
        } else if (constSymbol.type() instanceof ArrayType constSymbolType) {
            Pair<IRType, Constant<?>> optimizedArray = optimizeGlobalArray(constSymbolType, constSymbol.initVals());
            globalConstant = new GlobalVariable(constSymbol.name(), optimizedArray.key(),
                    true, false,
                    optimizedArray.value());
        } else {
            throw new RuntimeException("When addGlobalConstant(), illegal type. Got " + constSymbol.type() +
                    ", expected IntegerType or ArrayType");
        }
        irModule.appendGlobalVariables(globalConstant);
        return globalConstant;
    }

    GlobalVariable addGlobalVariable(VarSymbol varSymbol, ArrayList<Integer> initVals) {
        GlobalVariable globalConstant;
        if (varSymbol.type() instanceof IntegerType varSymbolType) {
            globalConstant = new GlobalVariable(varSymbol.name(), varSymbol.type(),
                    false, false,
                    new ConstantInt(varSymbolType, initVals.get(0)));
        } else if (varSymbol.type() instanceof ArrayType varSymbolType) {
            Pair<IRType, Constant<?>> optimizedArray = optimizeGlobalArray(varSymbolType, initVals);
            globalConstant = new GlobalVariable(varSymbol.name(), optimizedArray.key(),
                    false, false,
                    optimizedArray.value());
        } else {
            throw new RuntimeException("When addGlobalVariable(), illegal type. Got " + varSymbol.type() +
                    ", expected IntegerType or ArrayType");
        }
        irModule.appendGlobalVariables(globalConstant);
        return globalConstant;
    }

    private static Pair<IRType, Constant<?>> optimizeGlobalArray(ArrayType originType, ArrayList<Integer> originInitVals) {
        // 寻找最后一个不是0的数字
        int lastNotZero = originInitVals.size() - 1;
        while (lastNotZero >= 0 && originInitVals.get(lastNotZero) == 0) {
            lastNotZero--;
        }
        if (lastNotZero == -1) {
            // 如果全是0，那么直接使用zeroinitializer
            return new Pair<>(originType, new ConstantAggregateZero(originType));
        } else if (Config.disableLongArrayOptimization || originInitVals.size() - 1 - lastNotZero < 10) {
            // 如果不对长数组做优化或者结尾非0的数据不足10个，那么保留原样
            ArrayList<Constant<?>> constantValues = new ArrayList<>(convertInitValIntegerArray(originType, originInitVals, -1));
            return new Pair<>(originType, new ConstantArray(originType, constantValues));
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
            ArrayList<Constant<?>> constantValues = new ArrayList<>(convertInitValIntegerArray(originType, originInitVals, lastNotZero));
            // 均为0的部分使用zeroinitializer
            constantValues.add(new ConstantAggregateZero(zeroPartType));
            return new Pair<>(structType, new ConstantStruct(structType, constantValues));
        }
    }

    private static ArrayList<ConstantInt> convertInitValIntegerArray(ArrayType type, ArrayList<Integer> initVals, int endIndex) {
        ArrayList<ConstantInt> constantValues = new ArrayList<>();
        if (endIndex < 0) {
            endIndex = initVals.size() - 1;
        } else if (endIndex >= initVals.size()) {
            throw new IndexOutOfBoundsException("When convertInitValIntegerArray(), endIndex is greater than the size of initVals. " +
                    "Got " + endIndex + ", expected " + initVals.size());
        }
        for (int i = 0; i <= endIndex; i++) {
            // CAST 由于BType只有int和char，此处强制转换不会出错
            constantValues.add(new ConstantInt((IntegerType) type.elementType(), initVals.get(i)));
        }
        return constantValues;
    }

    Function addFunction(FuncSymbol funcSymbol, ArrayList<Argument> arguments) {
        Function function = new Function(funcSymbol.name(), funcSymbol.type(), arguments);
        irModule.appendFunctions(function);
        this.nowFunction = function;
        return function;
    }

    void addMainFunction() {
        // 主函数返回值为0，没有参数
        Function mainFunction = new Function("main", new FunctionType(IRType.getInt32Ty(), new ArrayList<>()),
                new ArrayList<>());
        irModule.appendFunctions(mainFunction);
        this.nowFunction = mainFunction;
    }

    BasicBlock newBasicBlock() {
        BasicBlock basicBlock = new BasicBlock(this.nowFunction);
        this.nowFunction.appendBasicBlock(basicBlock);
        return basicBlock;
    }

    AllocaInst addArgument(Argument argument, BasicBlock entryBlock) {
        AllocaInst allocaInst = new AllocaInst(argument.type(), entryBlock);
        new StoreInst(argument, allocaInst, entryBlock);
        return allocaInst;
    }

    AllocaInst addLocalConstant(ConstSymbol constSymbol, BasicBlock entryBlock) {
        AllocaInst allocaInst = new AllocaInst(constSymbol.type(), entryBlock);
        if (constSymbol.type() instanceof IntegerType constSymbolType) {
            new StoreInst(new ConstantInt(constSymbolType, constSymbol.initVals().get(0)), allocaInst, entryBlock);
        } else if (constSymbol.type() instanceof ArrayType constSymbolType) {
            // TODO 对于长数组进行优化
            for (int i = 0; i < constSymbol.initVals().size(); i++) {
                GetElementPtrInst arrayElementPointer =
                        this.addGetArrayElementPointer(allocaInst, new ConstantInt(IRType.getInt32Ty(), i), entryBlock);
                // CAST 由于BType只有int和char，此处强制转换不会出错
                new StoreInst(new ConstantInt((IntegerType) constSymbolType.elementType(), constSymbol.initVals().get(i)),
                        arrayElementPointer, entryBlock);
            }
        } else {
            throw new RuntimeException("When addLocalConstant(), illegal type. Got " + constSymbol.type() +
                    ", expected IntegerType or ArrayType");
        }
        return allocaInst;
    }

    AllocaInst addLocalVariable(VarSymbol varSymbol, ArrayList<IRValue<IntegerType>> initVals, BasicBlock entryBlock) {
        AllocaInst allocaInst = new AllocaInst(varSymbol.type(), entryBlock);
        if (initVals != null) {
            if (varSymbol.type() instanceof IntegerType) {
                IRValue<IntegerType> initVal = initVals.get(0);
                this.storeLVal(initVal, allocaInst, entryBlock);
            } else if (varSymbol.type() instanceof ArrayType) {
                // TODO 对于长数组进行优化
                for (int i = 0; i < initVals.size(); i++) {
                    GetElementPtrInst arrayElementPointer =
                            this.addGetArrayElementPointer(allocaInst, new ConstantInt(IRType.getInt32Ty(), i), entryBlock);
                    // CAST 由于BType只有int和char，此处强制转换不会出错
                    this.storeLVal(initVals.get(i), arrayElementPointer, entryBlock);
                }
            } else {
                throw new RuntimeException("When addLocalVariable(), illegal type. Got " + varSymbol.type() +
                        ", expected IntegerType or ArrayType");
            }
        }
        return allocaInst;
    }

    GetElementPtrInst addGetArrayElementPointer(IRValue<PointerType> pointer, IRValue<IntegerType> index, BasicBlock insertBlock) {
        // 在SysY中，只有一维数组，访问时就分为两种情况
        if (pointer.type().referenceType() instanceof ArrayType || pointer.type().referenceType() instanceof PointerType) {
            // ArrayType是常量和变量的，PointerType是参数的
            return new GetElementPtrInst(pointer, new ArrayList<>(Arrays.asList(ConstantInt.ZERO_I32(), index)), insertBlock);
        } else {
            throw new RuntimeException("When addGetArrayElementPointer(), illegal type. Got " + pointer.type() +
                    ", expected ArrayType or PointerType");
        }
    }

    void storeLVal(IRValue<IntegerType> value, IRValue<PointerType> lValAddress, BasicBlock insertBlock) {
        // 在赋值的地址不为空的时候才创建StoreInst
        if (lValAddress != null) {
            if (lValAddress.type().referenceType() instanceof IntegerType lValType) {
                if (value.type().size() < lValType.size()) {
                    // 短值向长值
                    value = this.addExtendOperation(value, lValType, insertBlock);
                } else if (value.type().size() > lValType.size()) {
                    // 长值向短值
                    value = this.addTruncOperation(value, lValType, insertBlock);
                }
            } else {
                throw new RuntimeException("When storeLVal(), the lValAddress is not a pointer to IntegerType. " +
                        "Got " + lValAddress.type() + " lValAddress " + lValAddress);
            }
            new StoreInst(value, lValAddress, insertBlock);
        }
    }

    LoadInst loadLVal(IRValue<PointerType> lValAddress, BasicBlock insertBlock) {
        return new LoadInst(lValAddress, insertBlock);
    }

    GetElementPtrInst loadConstStringPointer(ArrayList<Integer> strChar, BasicBlock insertBlock) {
        StringBuilder sb = new StringBuilder();
        strChar.forEach(c -> sb.append((char) c.byteValue()));
        String str = sb.toString();
        if (!this.constStr.containsKey(str)) {
            ArrayType constStrArrayType = new ArrayType(IRType.getInt8Ty(), strChar.size());
            ConstantArray constStrArray = new ConstantArray(constStrArrayType,
                    new ArrayList<>(strChar.stream().map(c -> new ConstantInt(IRType.getInt8Ty(), c)).toList()));
            GlobalVariable constStrGlobalVariable =
                    new GlobalVariable(".str." + this.constStr.size(), constStrArrayType,
                            true, true,
                            constStrArray);
            irModule.appendGlobalVariables(constStrGlobalVariable);
            this.constStr.put(str, constStrGlobalVariable);
        }
        return this.addGetArrayElementPointer(this.constStr.get(str), ConstantInt.ZERO_I32(), insertBlock);
    }

    BinaryOperator addBinaryOperation(Token symbol, IRValue<IntegerType> value1, IRValue<IntegerType> value2, BasicBlock insertBlock) {
        return switch (symbol.type()) {
            case PLUS -> new BinaryOperator(BinaryOperator.BinaryOps.ADD, value1, value2, insertBlock);
            case MINU -> new BinaryOperator(BinaryOperator.BinaryOps.SUB, value1, value2, insertBlock);
            case MULT -> new BinaryOperator(BinaryOperator.BinaryOps.MUL, value1, value2, insertBlock);
            case DIV -> new BinaryOperator(BinaryOperator.BinaryOps.DIV, value1, value2, insertBlock);
            case MOD -> new BinaryOperator(BinaryOperator.BinaryOps.MOD, value1, value2, insertBlock);
            default ->
                    throw new RuntimeException("When addBinaryOperation(), illegal symbol type. Got " + symbol.type());
        };
    }

    CallInst addCallFunction(Function function, ArrayList<IRValue<?>> arguments, BasicBlock insertBlock) {
        // addCallFunction不处理对于函数不合法的调用，由Visitor予以检查
        return new CallInst(function, arguments, insertBlock);
    }

    CallInst addCallLibFunction(String functionName, ArrayList<IRValue<?>> arguments, BasicBlock insertBlock) {
        // addCallLibFunction不处理对于函数不合法的调用，由Visitor予以检查
        if (this.libFunctions.containsKey(functionName)) {
            return new CallInst(this.libFunctions.get(functionName), arguments, insertBlock);
        } else {
            throw new RuntimeException("When addCallLibFunction(), illegal function name. Got " + functionName);
        }
    }

    CastInst.TruncInst addTruncOperation(IRValue<IntegerType> src, IntegerType destType, BasicBlock insertBlock) {
        return new CastInst.TruncInst(src, destType, insertBlock);
    }

    CastInst.ZExtInst addExtendOperation(IRValue<IntegerType> src, IntegerType destType, BasicBlock insertBlock) {
        return new CastInst.ZExtInst(src, destType, insertBlock);
    }

    <D extends IRType> CastInst.BitCastInst<D> addBitCastOperation(IRValue<?> src, D destType, BasicBlock insertBlock) {
        return new CastInst.BitCastInst<>(src, destType, insertBlock);
    }

    void addReturnInstruction(IRValue<IntegerType> returnValue, BasicBlock insertBlock) {
        new ReturnInst(returnValue, insertBlock);
    }
}
