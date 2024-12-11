package frontend.visitor;

import IR.*;
import IR.type.*;
import IR.value.*;
import IR.value.constant.*;
import IR.value.instruction.*;
import frontend.lexer.Token;
import frontend.type.TokenType;
import frontend.visitor.symbol.*;
import global.Config;
import util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

class Builder {
    private final IRModule irModule;

    // 库函数
    private final HashMap<String, IRFunction> libFunctions;
    // 静态字符串
    private final HashMap<String, IRGlobalVariable> constStr;
    // 当前访问的函数
    private IRFunction nowFunction = null;

    Builder(IRModule irModule) {
        this.irModule = irModule;
        this.libFunctions = getLibFunctions();
        this.constStr = new HashMap<>();
        // 将库函数的定义添加到IRModule中
        this.libFunctions.forEach((name, function) -> irModule.appendFunctions(function));
    }

    private static HashMap<String, IRFunction> getLibFunctions() {
        HashMap<String, IRFunction> libFunctions = new HashMap<>();
        // 库函数均为声明，无需给出Argument
        ArrayList<IRType> parameters;
        // declare i32 @getint()      读取一个整数
        libFunctions.put("getint", new IRFunction("getint", new FunctionType(IRType.getInt32Ty(), new ArrayList<>())));
        // declare i32 @getchar()     读取一个字符
        libFunctions.put("getchar", new IRFunction("getchar", new FunctionType(IRType.getInt32Ty(), new ArrayList<>())));
        // declare void @putint(i32)  输出一个整数
        parameters = new ArrayList<>(Collections.singletonList(IRType.getInt32Ty()));
        libFunctions.put("putint", new IRFunction("putint", new FunctionType(IRType.getVoidTy(), parameters)));
        // declare void @putch(i32)   输出一个字符
        parameters = new ArrayList<>(Collections.singletonList(IRType.getInt32Ty()));
        libFunctions.put("putch", new IRFunction("putch", new FunctionType(IRType.getVoidTy(), parameters)));
        // declare void @putstr(i8*)  输出字符串
        parameters = new ArrayList<>(Collections.singletonList(new PointerType(IRType.getInt8Ty(), false)));
        libFunctions.put("putstr", new IRFunction("putstr", new FunctionType(IRType.getVoidTy(), parameters)));
        return libFunctions;
    }

    IRGlobalVariable addGlobalConstant(ConstSymbol constSymbol) {
        IRGlobalVariable globalConstant;
        if (constSymbol.type() instanceof IntegerType constSymbolType) {
            globalConstant = new IRGlobalVariable(constSymbol.name(), constSymbol.type(),
                    true, false,
                    new ConstantInt(constSymbolType, constSymbol.initVals().get(0)));
        } else if (constSymbol.type() instanceof ArrayType constSymbolType) {
            Pair<IRType, IRConstant<?>> optimizedArray = optimizeGlobalArray(constSymbolType, constSymbol.initVals());
            globalConstant = new IRGlobalVariable(constSymbol.name(), optimizedArray.key(),
                    true, false,
                    optimizedArray.value());
        } else {
            throw new RuntimeException("When addGlobalConstant(), illegal type. Got " + constSymbol.type() +
                    ", expected IntegerType or ArrayType");
        }
        irModule.appendGlobalVariables(globalConstant);
        return globalConstant;
    }

    IRGlobalVariable addGlobalVariable(VarSymbol varSymbol, ArrayList<Integer> initVals) {
        IRGlobalVariable globalConstant;
        if (varSymbol.type() instanceof IntegerType varSymbolType) {
            globalConstant = new IRGlobalVariable(varSymbol.name(), varSymbol.type(),
                    false, false,
                    new ConstantInt(varSymbolType, initVals.get(0)));
        } else if (varSymbol.type() instanceof ArrayType varSymbolType) {
            Pair<IRType, IRConstant<?>> optimizedArray = optimizeGlobalArray(varSymbolType, initVals);
            globalConstant = new IRGlobalVariable(varSymbol.name(), optimizedArray.key(),
                    false, false,
                    optimizedArray.value());
        } else {
            throw new RuntimeException("When addGlobalVariable(), illegal type. Got " + varSymbol.type() +
                    ", expected IntegerType or ArrayType");
        }
        irModule.appendGlobalVariables(globalConstant);
        return globalConstant;
    }

    private static Pair<IRType, IRConstant<?>> optimizeGlobalArray(ArrayType originType, ArrayList<Integer> originInitVals) {
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
            ArrayList<IRConstant<?>> constantValues = new ArrayList<>(convertInitValIntegerArray(originType, originInitVals, -1));
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
            ArrayList<IRConstant<?>> constantValues = new ArrayList<>(convertInitValIntegerArray(originType, originInitVals, lastNotZero));
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

    IRFunction addFunction(FuncSymbol funcSymbol, ArrayList<Argument> arguments) {
        IRFunction function = new IRFunction(funcSymbol.name(), funcSymbol.type(), arguments);
        irModule.appendFunctions(function);
        this.nowFunction = function;
        return function;
    }

    void addMainFunction() {
        // 主函数返回值为0，没有参数
        IRFunction mainFunction = new IRFunction("main", new FunctionType(IRType.getInt32Ty(), new ArrayList<>()),
                new ArrayList<>());
        irModule.appendFunctions(mainFunction);
        this.nowFunction = mainFunction;
    }

    IRBasicBlock newBasicBlock() {
        return new IRBasicBlock(this.nowFunction);
    }

    void appendBasicBlock(IRBasicBlock basicBlock) {
        if (Objects.equals(this.nowFunction, basicBlock.parent())) {
            this.nowFunction.appendBasicBlock(basicBlock);
        } else {
            throw new RuntimeException("When appendBasicBlock(), parent of basicBlock is not nowFunction. " +
                    "Got " + basicBlock.parent().name() + ", expected " + this.nowFunction.name());
        }
    }

    AllocaInst addArgument(ArgSymbol argSymbol, IRBasicBlock defBlock) {
        AllocaInst allocaInst = new AllocaInst(argSymbol.type(), defBlock);
        new StoreInst(argSymbol.argument(), allocaInst, defBlock);
        return allocaInst;
    }

    AllocaInst addLocalConstant(ConstSymbol constSymbol, IRBasicBlock defBlock) {
        AllocaInst allocaInst = new AllocaInst(constSymbol.type(), defBlock);
        if (constSymbol.type() instanceof IntegerType constSymbolType) {
            new StoreInst(new ConstantInt(constSymbolType, constSymbol.initVals().get(0)), allocaInst, defBlock);
        } else if (constSymbol.type() instanceof ArrayType constSymbolType) {
            for (int i = 0; i < constSymbol.initVals().size(); i++) {
                GetElementPtrInst arrayElementPointer =
                        this.addGetArrayElementPointer(allocaInst, new ConstantInt(IRType.getInt32Ty(), i), defBlock);
                // CAST 由于BType只有int和char，此处强制转换不会出错
                new StoreInst(new ConstantInt((IntegerType) constSymbolType.elementType(), constSymbol.initVals().get(i)),
                        arrayElementPointer, defBlock);
            }
        } else {
            throw new RuntimeException("When addLocalConstant(), illegal type. Got " + constSymbol.type() +
                    ", expected IntegerType or ArrayType");
        }
        return allocaInst;
    }

    AllocaInst addLocalVariable(VarSymbol varSymbol, ArrayList<IRValue<IntegerType>> initVals,
                                IRBasicBlock defBlock, IRBasicBlock insertBlock) {
        AllocaInst allocaInst = new AllocaInst(varSymbol.type(), defBlock);
        if (initVals != null) {
            if (varSymbol.type() instanceof IntegerType) {
                IRValue<IntegerType> initVal = initVals.get(0);
                this.storeLVal(initVal, allocaInst, insertBlock);
            } else if (varSymbol.type() instanceof ArrayType) {
                for (int i = 0; i < initVals.size(); i++) {
                    GetElementPtrInst arrayElementPointer =
                            this.addGetArrayElementPointer(allocaInst, new ConstantInt(IRType.getInt32Ty(), i), insertBlock);
                    // CAST 由于BType只有int和char，此处强制转换不会出错
                    this.storeLVal(initVals.get(i), arrayElementPointer, insertBlock);
                }
            } else {
                throw new RuntimeException("When addLocalVariable(), illegal type. Got " + varSymbol.type() +
                        ", expected IntegerType or ArrayType");
            }
        }
        return allocaInst;
    }

    IRGlobalVariable getConstStringPointer(ArrayList<Integer> strChar) {
        StringBuilder sb = new StringBuilder();
        strChar.forEach(c -> sb.append((char) c.byteValue()));
        String str = sb.toString();
        if (!this.constStr.containsKey(str)) {
            ArrayType constStrArrayType = new ArrayType(IRType.getInt8Ty(), strChar.size());
            ConstantArray constStrArray = new ConstantArray(constStrArrayType,
                    new ArrayList<>(strChar.stream().map(c -> new ConstantInt(IRType.getInt8Ty(), c)).toList()));
            IRGlobalVariable constStrGlobalVariable =
                    new IRGlobalVariable(".str." + this.constStr.size(), constStrArrayType,
                            true, true,
                            constStrArray);
            irModule.appendGlobalVariables(constStrGlobalVariable);
            this.constStr.put(str, constStrGlobalVariable);
        }
        return this.constStr.get(str);
    }

    GetElementPtrInst addGetArrayElementPointer(IRValue<PointerType> pointer, IRValue<IntegerType> index, IRBasicBlock insertBlock) {
        // 在SysY中，只有一维数组，访问时就分为两种情况
        if (pointer.type().referenceType() instanceof ArrayType) {
            // ArrayType是常量和变量的
            return new GetElementPtrInst(pointer, new ArrayList<>(Arrays.asList(ConstantInt.ZERO_I32(), index)), insertBlock);
        } else if (pointer.type().referenceType() instanceof PointerType) {
            // PointerType是参数的
            // CAST 上方的instanceof确保转换正确
            IRValue<PointerType> referencedPointer = IRValue.cast(new LoadInst(pointer, insertBlock));
            return new GetElementPtrInst(referencedPointer, new ArrayList<>(Collections.singletonList(index)), insertBlock);
        } else {
            throw new RuntimeException("When addGetArrayElementPointer(), illegal type. Got " + pointer.type() +
                    ", expected ArrayType or PointerType");
        }
    }

    void storeLVal(IRValue<IntegerType> value, IRValue<PointerType> lValAddress, IRBasicBlock insertBlock) {
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

    LoadInst loadLVal(IRValue<PointerType> lValAddress, IRBasicBlock insertBlock) {
        return new LoadInst(lValAddress, insertBlock);
    }

    BinaryOperator addBinaryOperation(Token symbol, IRValue<IntegerType> value1, IRValue<IntegerType> value2, IRBasicBlock insertBlock) {
        // 自动处理类型转换
        if (value1.type().size() < IRType.getInt32Ty().size()) {
            value1 = this.addExtendOperation(value1, IRType.getInt32Ty(), insertBlock);
        }
        if (value2.type().size() < IRType.getInt32Ty().size()) {
            value2 = this.addExtendOperation(value2, IRType.getInt32Ty(), insertBlock);
        }
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

    CallInst addCallFunction(IRFunction function, ArrayList<IRValue<?>> arguments, IRBasicBlock insertBlock) {
        // addCallFunction不处理对于函数不合法的调用，由Visitor予以检查
        return new CallInst(function, arguments, insertBlock);
    }

    CallInst addCallLibFunction(String functionName, ArrayList<IRValue<?>> arguments, IRBasicBlock insertBlock) {
        // addCallLibFunction不处理对于函数不合法的调用，由Visitor予以检查
        if (this.libFunctions.containsKey(functionName)) {
            return new CallInst(this.libFunctions.get(functionName), arguments, insertBlock);
        } else {
            throw new RuntimeException("When addCallLibFunction(), illegal function name. Got " + functionName);
        }
    }

    CastInst.TruncInst addTruncOperation(IRValue<IntegerType> src, IntegerType destType, IRBasicBlock insertBlock) {
        return new CastInst.TruncInst(src, destType, insertBlock);
    }

    CastInst.ZExtInst addExtendOperation(IRValue<IntegerType> src, IntegerType destType, IRBasicBlock insertBlock) {
        return new CastInst.ZExtInst(src, destType, insertBlock);
    }

    <D extends IRType> CastInst.BitCastInst<D> addBitCastOperation(IRValue<?> src, D destType, IRBasicBlock insertBlock) {
        return new CastInst.BitCastInst<>(src, destType, insertBlock);
    }

    IcmpInst addIcmpOperation(Token symbol, IRValue<IntegerType> value1, IRValue<IntegerType> value2, IRBasicBlock insertBlock) {
        if (value2 != null) {
            // 自动处理类型转换
            if (value1.type().size() < value2.type().size()) {
                value1 = this.addExtendOperation(value1, value2.type(), insertBlock);
            } else if (value1.type().size() > value2.type().size()) {
                value2 = this.addExtendOperation(value2, value1.type(), insertBlock);
            }
            return switch (symbol.type()) {
                case LSS -> new IcmpInst(IcmpInst.Predicate.LT, value1, value2, insertBlock); // <
                case GRE -> new IcmpInst(IcmpInst.Predicate.GT, value1, value2, insertBlock); // >
                case LEQ -> new IcmpInst(IcmpInst.Predicate.LE, value1, value2, insertBlock); // <=
                case GEQ -> new IcmpInst(IcmpInst.Predicate.GE, value1, value2, insertBlock); // >=
                case EQL -> new IcmpInst(IcmpInst.Predicate.EQ, value1, value2, insertBlock); // ==
                case NEQ -> new IcmpInst(IcmpInst.Predicate.NE, value1, value2, insertBlock); // !=
                default ->
                        throw new RuntimeException("When addIcmpOperation(), illegal symbol type. Got " + symbol.type());
            };
        } else {
            if (symbol == null) {
                // 如果EqExp层和RelExp层都没有做过比较那么在离开EqExp层需要做一次比较
                return new IcmpInst(IcmpInst.Predicate.NE, value1, new ConstantInt(value1.type(), 0), insertBlock);
            } else if (symbol.type() == TokenType.NOT) {
                // UnaryExp中的'!' UnaryExp
                return new IcmpInst(IcmpInst.Predicate.EQ, value1, new ConstantInt(value1.type(), 0), insertBlock);
            } else {
                throw new RuntimeException("When addIcmpOperation(), value2 is null but the symbol type is not " + TokenType.NOT);
            }
        }
    }

    void addReturnInstruction(IRValue<IntegerType> returnValue, IRType returnType, IRBasicBlock insertBlock) {
        if (returnValue != null && returnType instanceof IntegerType returnIntegerType) {
            if (returnValue.type().size() < returnIntegerType.size()) {
                // 短值向长值
                returnValue = this.addExtendOperation(returnValue, returnIntegerType, insertBlock);
            } else if (returnValue.type().size() > returnIntegerType.size()) {
                // 长值向短值
                returnValue = this.addTruncOperation(returnValue, returnIntegerType, insertBlock);
            }
        }
        new ReturnInst(returnValue, insertBlock);
    }

    void addBranchInstruction(IRValue<IntegerType> cond, IRBasicBlock trueBlock, IRBasicBlock falseBlock, IRBasicBlock insertBlock) {
        if (cond == null && falseBlock == null) {
            new BranchInst(trueBlock, insertBlock);
        } else if (cond == null || falseBlock == null) {
            throw new RuntimeException("When addBranchInstruction(), cond is null or falseBlock is null, but not both of them are null.");
        } else {
            new BranchInst(cond, trueBlock, falseBlock, insertBlock);
        }
    }
}
