package frontend.visitor;

import IR.IRModule;
import IR.IRValue;
import IR.type.*;
import IR.value.BasicBlock;
import IR.value.constant.ConstantInt;
import frontend.error.ErrorTable;
import frontend.error.ErrorType;
import frontend.lexer.Token;
import frontend.parser.CompUnit;
import frontend.parser.declaration.*;
import frontend.parser.declaration.constant.*;
import frontend.parser.declaration.function.*;
import frontend.parser.declaration.variable.*;
import frontend.parser.expression.*;
import frontend.parser.statement.*;
import frontend.type.TokenType;
import frontend.visitor.symbol.*;

import java.util.ArrayList;
import java.util.Collections;

public class Visitor {
    private final CompUnit compUnit;
    private boolean finish = false;

    private final SymbolTable symbolTable;
    private final Calculator calculator;
    private final Builder builder;

    private final ErrorTable errorTable;

    private final IRModule irModule;

    public Visitor(CompUnit compUnit, ErrorTable errorTable) {
        this.compUnit = compUnit;
        this.errorTable = errorTable;
        this.symbolTable = new SymbolTable(errorTable);
        this.calculator = new Calculator(this.symbolTable);
        this.irModule = new IRModule();
        this.builder = new Builder(this.irModule);
    }

    public SymbolTable getSymbolTable() {
        return this.symbolTable;
    }


    // CompUnit → {Decl} {FuncDef} MainFuncDef
    public IRModule visitCompUnit() {
        if (finish) {
            return this.irModule;
        }
        // 全局符号表进入
        symbolTable.push();
        // 全局变量
        this.compUnit.decls().forEach(this::visitGlobalDecl);
        // 各函数
        this.compUnit.funcDefs().forEach(this::visitFuncDef);
        // 主函数
        this.visitMainFuncDef(this.compUnit.mainFuncDef());
        // 全局符号表弹出
        symbolTable.pop();
        this.finish = true;
        return this.irModule;
    }

    // Decl → ConstDecl | VarDecl
    private void visitGlobalDecl(Decl decl) {
        if (decl instanceof ConstDecl constDecl) {
            for (ConstSymbol constSymbol : this.visitConstDecl(constDecl)) {
                constSymbol.setIRValue(this.builder.addGlobalConstant(constSymbol));
            }
        } else if (decl instanceof VarDecl varDecl) {
            this.visitGlobalVarDecl(varDecl);
        } else {
            throw new RuntimeException("When visitGlobalDecl(), got unknown type of Decl (" + decl.getClass().getSimpleName() + ")");
        }
    }

    // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    // FuncType → 'void' | 'int' | 'char'
    private void visitFuncDef(FuncDef funcDef) {
        // visitFuncType()
        Token funcType = funcDef.funcType().typeToken();
        Token ident = funcDef.ident();
        ArrayList<VarSymbol> parameters;
        if (funcDef.funcFParams() == null) {
            // 无参数
            parameters = new ArrayList<>();
        } else {
            // 有参数，在此处即遍历FuncFParams，先用于生成函数类型
            parameters = this.visitFuncFParams(funcDef.funcFParams());
        }
        FuncSymbol newSymbol = new FuncSymbol(Translator.getFuncIRType(funcType,
                new ArrayList<>(parameters.stream().map(VarSymbol::type).toList())), ident);
        newSymbol.setIRValue(this.builder.addFunction(newSymbol, parameters));
        // 函数尝试进入符号表，为了检查出尽可能多的错误，无论是否成功都继续操作以检查
        this.symbolTable.insert(newSymbol);
        // 进入函数的作用域
        this.symbolTable.push();
        // 参数尝试进入符号表
        for (VarSymbol parameter : parameters) {
            this.symbolTable.insert(parameter);
        }
        this.visitFunctionBlock(funcDef.block().blockItems());
        // 离开函数的作用域
        this.symbolTable.pop();
    }

    // MainFuncDef → 'int' 'main' '(' ')' Block
    private void visitMainFuncDef(MainFuncDef mainFuncDef) {
        // 主函数不需要进入符号表，也没有参数
        this.builder.addMainFunction();
        // 进入主函数的作用域
        this.symbolTable.push();
        this.visitFunctionBlock(mainFuncDef.block().blockItems());
        // 离开主函数的作用域
        this.symbolTable.pop();
    }

    // ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
    // BType → 'int' | 'char'
    // ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
    private ArrayList<ConstSymbol> visitConstDecl(ConstDecl constDecl) {
        ArrayList<ConstSymbol> ret = new ArrayList<>();
        Token bType = constDecl.typeToken();
        // visitConstDef()
        for (ConstDef constDef : constDecl.constDefs()) {
            Token ident = constDef.ident();
            ConstSymbol newSymbol;
            ArrayList<Integer> initVals;
            // 必定有初始值
            initVals = this.visitConstInitVal(constDef.constInitVal());
            // 字符型截取低八位
            if (bType.type() == TokenType.CHARTK) {
                initVals.replaceAll(integer -> (int) integer.byteValue());
            }
            if (constDef.constExp() == null) {
                // 非数组
                if (initVals.size() != 1) {
                    throw new RuntimeException("When visitConstDecl(), initVals of identifier " + ident + " mismatch its type");
                }
                newSymbol = new ConstSymbol(Translator.getConstIRType(bType, null), ident, initVals);
            } else {
                // 数组
                int length = this.calculator.calculateConstExp(constDef.constExp());
                if (initVals.size() > length) {
                    throw new RuntimeException("When visitConstDecl(), initVals of identifier " + ident + " is longer than its length");
                } else {
                    // 补齐未显示写出的0
                    for (int i = initVals.size(); i < length; i++) {
                        initVals.add(0);
                    }
                }
                newSymbol = new ConstSymbol(Translator.getConstIRType(bType, length), ident, initVals);
            }
            if (this.symbolTable.insert(newSymbol)) {
                ret.add(newSymbol);
            }
        }
        return ret;
    }

    // ConstInitVal → ConstExp | '{' [ ConstExp { ',' ConstExp } ] '}' | StringConst
    private ArrayList<Integer> visitConstInitVal(ConstInitVal constInitVal) {
        if (constInitVal.getType() == ConstInitVal.Type.BASIC) {
            return new ArrayList<>(Collections.singletonList(this.calculator.calculateConstExp(constInitVal.constExp())));
        } else if (constInitVal.getType() == ConstInitVal.Type.STRING) {
            return Translator.translateStringConst(constInitVal.stringConst());
        } else if (constInitVal.getType() == ConstInitVal.Type.ARRAY) {
            // 由于只考虑一维数组，所以此处直接解析计算
            return new ArrayList<>(constInitVal.constExps().stream().map(this.calculator::calculateConstExp).toList());
        } else {
            throw new RuntimeException("When visitConstInitVal(), got unknown type of ConstInitVal (" + constInitVal.getType() + ")");
        }
    }

    // VarDecl → BType VarDef { ',' VarDef } ';'
    // BType → 'int' | 'char'
    // VarDef → Ident [ '[' ConstExp ']' ] [ '=' InitVal ]
    private void visitGlobalVarDecl(VarDecl varDecl) {
        Token bType = varDecl.typeToken();
        // visitVarDef()
        for (VarDef varDef : varDecl.varDefs()) {
            Token ident = varDef.ident();
            VarSymbol newSymbol;
            ArrayList<Integer> initVals;
            // 全局变量必有初始值
            if (varDef.initVal() == null) {
                // 无初始值
                initVals = new ArrayList<>();
                if (varDef.constExp() == null) {
                    // 不是数组的给一个0，是数组的直接利用补齐0，因为数组的长度可以等于0，所以不能直接所有都给一个0
                    initVals.add(0);
                }
            } else {
                // 有初始值
                initVals = this.visitGlobalInitVal(varDef.initVal());
            }
            if (varDef.constExp() == null) {
                // 非数组
                if (initVals.size() != 1) {
                    throw new RuntimeException("When visitGlobalVarDecl(), initVals of identifier " + ident + " mismatch its type");
                }
                newSymbol = new VarSymbol(Translator.getVarIRType(bType, null, false), ident);
            } else {
                // 数组
                Integer length = this.calculator.calculateConstExp(varDef.constExp());
                if (initVals.size() > length) {
                    throw new RuntimeException("When visitGlobalVarDecl(), initVals of identifier " + ident + " is longer than its length");
                } else {
                    // 补齐未显示写出的0
                    for (int i = initVals.size(); i < length; i++) {
                        initVals.add(0);
                    }
                }
                newSymbol = new VarSymbol(Translator.getVarIRType(bType, length, false), ident);
            }
            // 上方已经对初始值进行了分析，为了避免LLVM IR错误，判断了成功插入再添加GlobalVariable
            if (this.symbolTable.insert(newSymbol)) {
                this.builder.addGlobalVariable(newSymbol, initVals);
            }
        }
    }

    // InitVal → Exp | '{' [ Exp { ',' Exp } ] '}' | StringConst
    private ArrayList<Integer> visitGlobalInitVal(InitVal initVal) {
        if (initVal.getType() == InitVal.Type.BASIC) {
            return new ArrayList<>(Collections.singletonList(this.calculator.calculateExp(initVal.exp())));
        } else if (initVal.getType() == InitVal.Type.STRING) {
            return Translator.translateStringConst(initVal.stringConst());
        } else if (initVal.getType() == InitVal.Type.ARRAY) {
            // 由于只考虑一维数组，所以此处直接解析计算
            return new ArrayList<>(initVal.exps().stream().map(this.calculator::calculateExp).toList());
        } else {
            throw new RuntimeException("When visitGlobalInitVal(), got unknown type of InitVal (" + initVal.getType() + ")");
        }
    }

    // FuncFParams → FuncFParam { ',' FuncFParam }
    //  FuncFParam → BType Ident ['[' ']']
    private ArrayList<VarSymbol> visitFuncFParams(FuncFParams funcFParams) {
        ArrayList<VarSymbol> ret = new ArrayList<>();
        for (FuncFParam funcFParam : funcFParams.funcFParams()) {
            if (funcFParam.getType() == FuncFParam.Type.BASIC) {
                ret.add(new VarSymbol(Translator.getVarIRType(funcFParam.typeToken(), null, false), funcFParam.ident()));
            } else if (funcFParam.getType() == FuncFParam.Type.ARRAY) {
                ret.add(new VarSymbol(Translator.getVarIRType(funcFParam.typeToken(), null, true), funcFParam.ident()));
            } else {
                throw new RuntimeException("When visitFuncFParams(), got unknown type of FuncFParam (" + funcFParam.getType() + ")");
            }
        }
        return ret;
    }

    // BlockItem → Decl | Stmt
    private void visitFunctionBlock(ArrayList<BlockItem> blockItems) {
        BasicBlock entryBlock = this.builder.newBasicBlock();
        for (BlockItem item : blockItems) {
            if (item instanceof Decl decl) {
                this.visitLocalDecl(decl, entryBlock);
            } else if (item instanceof Stmt stmt) {

            } else {
                throw new RuntimeException("When visitFunctionBlock(), got unknown type of BlockItem (" + item.getClass().getSimpleName() + ")");
            }
        }
    }

    // Decl → ConstDecl | VarDecl
    private void visitLocalDecl(Decl decl, BasicBlock entryBlock) {
        if (decl instanceof ConstDecl constDecl) {
            for (ConstSymbol constSymbol : this.visitConstDecl(constDecl)) {
                constSymbol.setIRValue(this.builder.addLocalConstant(constSymbol, entryBlock));
            }
        } else if (decl instanceof VarDecl varDecl) {
            this.visitLocalVarDecl(varDecl, entryBlock);
        } else {
            throw new RuntimeException("When visitLocalDecl(), got unknown type of Decl (" + decl.getClass().getSimpleName() + ")");
        }
    }

    // VarDecl → BType VarDef { ',' VarDef } ';'
    // BType → 'int' | 'char'
    // VarDef → Ident [ '[' ConstExp ']' ] [ '=' InitVal ]
    private void visitLocalVarDecl(VarDecl varDecl, BasicBlock entryBlock) {
        Token bType = varDecl.typeToken();
        // visitVarDef()
        for (VarDef varDef : varDecl.varDefs()) {
            Token ident = varDef.ident();
            VarSymbol newSymbol;
            if (varDef.constExp() == null) {
                // 非数组
                newSymbol = new VarSymbol(Translator.getVarIRType(bType, null, false), ident);
                if (varDef.initVal() != null) {
                    // 有初始值
                    if (varDef.initVal().getType() == InitVal.Type.BASIC) {

                    } else {
                        throw new RuntimeException("When visitLocalVarDecl(), initVals of identifier " + ident + " mismatch its type. " +
                                "Got " + varDef.initVal().getType() + ", expected " + InitVal.Type.BASIC);
                    }
                }
            } else {
                Integer length = this.calculator.calculateConstExp(varDef.constExp());
                newSymbol = new VarSymbol(Translator.getVarIRType(bType, length, false), ident);
                // 数组
                if (varDef.initVal() != null) {
                    // 有初始值
                    if (varDef.initVal().getType() == InitVal.Type.ARRAY) {

                    } else if (varDef.initVal().getType() == InitVal.Type.STRING) {

                    } else {
                        throw new RuntimeException("When visitLocalVarDecl(), initVals of identifier " + ident + " mismatch its type. " +
                                "Got " + varDef.initVal().getType() + ", expected " + InitVal.Type.ARRAY + "/" + InitVal.Type.STRING);
                    }
                }
            }
            // 即便插入不成功，生成了alloca指令也不会导致LLVM IR错误
            this.symbolTable.insert(newSymbol);
        }
    }

    // Exp → AddExp
    private IRValue visitExp(Exp exp, BasicBlock insertBlock) {
        return this.visitAddExp(exp.addExp(), insertBlock);
    }

    // AddExp → MulExp | AddExp ('+' | '−') MulExp
    private IRValue visitAddExp(AddExp addExp, BasicBlock insertBlock) {
        IRValue resultValue = this.visitMulExp(addExp.mulExps().get(0), insertBlock);
        for (int i = 0; i < addExp.symbols().size(); i++) {
            IRValue newValue = this.visitMulExp(addExp.mulExps().get(i + 1), insertBlock);
            // 注意，下层如果返回指针类型也是可以加减的，但是要使用GetElementPtr
            // 此处直接交由BinaryOperator做报错处理
            resultValue = this.builder.addBinaryOperation(addExp.symbols().get(i), resultValue, newValue, insertBlock);
        }
        return resultValue;
    }

    // MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    private IRValue visitMulExp(MulExp mulExp, BasicBlock insertBlock) {
        IRValue resultValue = this.visitUnaryExp(mulExp.unaryExps().get(0), insertBlock);
        for (int i = 0; i < mulExp.symbols().size(); i++) {
            IRValue newValue = this.visitUnaryExp(mulExp.unaryExps().get(i + 1), insertBlock);
            resultValue = this.builder.addBinaryOperation(mulExp.symbols().get(i), resultValue, newValue, insertBlock);
        }
        return resultValue;
    }

    // UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    // UnaryOp → '+' | '−' | '!'
    // FuncRParams → Exp { ',' Exp }
    private IRValue visitUnaryExp(UnaryExp unaryExp, BasicBlock insertBlock) {
        UnaryExp.UnaryExpOption unaryExpExtract = unaryExp.extract();
        if (unaryExpExtract instanceof UnaryExp.UnaryExp_UnaryOp unaryExp_unaryOp) {
            if (unaryExp_unaryOp.unaryOp().symbol().type() == TokenType.PLUS) {
                // 对于+直接提取内部值，但是对于不可运算的值此处不识别
                return this.visitUnaryExp(unaryExp_unaryOp.unaryExp(), insertBlock);
            } else if (unaryExp_unaryOp.unaryOp().symbol().type() == TokenType.MINU) {
                IRValue subValue = this.visitUnaryExp(unaryExp_unaryOp.unaryExp(), insertBlock);
                return this.builder.addBinaryOperation(unaryExp_unaryOp.unaryOp().symbol(),
                        ConstantInt.zero_i32(), subValue, insertBlock);
            } else {
                throw new RuntimeException("When visitUnaryExp(), got unexpected symbol " + unaryExp_unaryOp.unaryOp().symbol()
                        + ", expected " + TokenType.PLUS + "/" + TokenType.MINU);
            }
        } else if (unaryExpExtract instanceof UnaryExp.UnaryExp_PrimaryExp unaryExp_primaryExp) {
            return this.visitPrimaryExp(unaryExp_primaryExp.primaryExp(), insertBlock);
        } else if (unaryExpExtract instanceof UnaryExp.UnaryExp_IdentFuncCall unaryExp_identFuncCall) {
            Symbol<? extends IRType> searchedSymbol = this.symbolTable.searchOrError(unaryExp_identFuncCall.ident());
            if (searchedSymbol instanceof FuncSymbol funcSymbol) {
                ArrayList<IRValue> funcRParamsValues = this.visitFuncRParams(unaryExp_identFuncCall.funcRParams(),
                        unaryExp_identFuncCall.ident(), funcSymbol, insertBlock);
                // 检查函数的参数的数量和类型在visitFuncRParams中
                return this.builder.addCallFunction(funcSymbol.irValue(), funcRParamsValues, insertBlock);
            } else {
                // 查找不到符号，或者符号不是函数，强制置为0
                return ConstantInt.zero_i32();
            }
        } else {
            throw new RuntimeException("When visitUnaryExp(), got unknown type of UnaryExp ("
                    + unaryExpExtract.getClass().getSimpleName() + ")");
        }
    }

    private ArrayList<IRValue> visitFuncRParams(FuncRParams funcRParams, Token indentFuncCall,
                                                FuncSymbol funcSymbol, BasicBlock insertBlock) {
        // 将函数的参数解析为对应IRValue，同时检查函数的参数类型是否合法
        ArrayList<IRType> parametersType = funcSymbol.type().parametersType();
        ArrayList<IRValue> funcRParamsValues = new ArrayList<>(funcRParams.exps().stream()
                .map((exp) -> this.visitExp(exp, insertBlock)).toList());
        // 一行只有一个错误，不重复报错
        if (parametersType.size() != funcRParamsValues.size()) {
            // 函数的参数数量不对应，不做对应检查，直接报错并返回结果
            this.errorTable.addErrorRecord(indentFuncCall.line(), ErrorType.FUNCRPARAMS_NUM_MISMATCH,
                    "The function call " + indentFuncCall + " has a wrong parameters number. " +
                            "Got " + funcRParamsValues.size() + ", expected " + parametersType.size());
        } else {
            // 将实参翻译为形参需要的类型
            // 处理函数的参数类型
            for (int i = 0; i < funcRParamsValues.size(); i++) {
                IRType rType = funcRParamsValues.get(i).type();
                IRType fType = parametersType.get(i);
                if (rType instanceof VoidType) {
                    // 传递void函数给任何参数
                    this.errorTable.addErrorRecord(indentFuncCall.line(), ErrorType.FUNCRPARAM_TYPE_MISMATCH,
                            "The function call" + indentFuncCall + "has a parameter of void function at " + (i + 1) + "parameter");
                } else if (rType instanceof PointerType && fType instanceof IntegerType
                        || rType instanceof IntegerType && fType instanceof PointerType) {
                    // 传递数组给变量或传递变量给数组，由于只有一维数组，形参和evaluation后的exp在是数组时都是PointerType
                    this.errorTable.addErrorRecord(indentFuncCall.line(), ErrorType.FUNCRPARAM_TYPE_MISMATCH,
                            "The function call " + indentFuncCall + " has a wrong parameter type at " + (i + 1) + " parameter");
                } else if (rType instanceof PointerType rArrayType && fType instanceof PointerType fArrayType) {
                    // 传递数组给数组，检查元素类型是否相同，不相同报错，相同则不用做任何特殊处理
                    if (!IRType.isEqual(rArrayType.referenceType(), fArrayType.referenceType())) {
                        this.errorTable.addErrorRecord(indentFuncCall.line(), ErrorType.FUNCRPARAM_TYPE_MISMATCH,
                                "The function call " + indentFuncCall + " has a wrong array element type at " + (i + 1) + " parameter");
                    }
                } else if (rType instanceof IntegerType rIntegerType && fType instanceof IntegerType fIntegerType) {
                    // 传递变量给变量，若size不相同要处理
                    if (rIntegerType.size() < fIntegerType.size()) {
                        // 实参小于形参，扩展
                        funcRParamsValues.set(i, this.builder.addExtendOperation(funcRParamsValues.get(i), fIntegerType, insertBlock));
                    } else if (rIntegerType.size() > fIntegerType.size()) {
                        // 实参大于形参，截断
                        funcRParamsValues.set(i, this.builder.addTruncOperation(funcRParamsValues.get(i), fIntegerType, insertBlock));
                    }
                }
            }
        }
        return funcRParamsValues;
    }

    // PrimaryExp → '(' Exp ')' | LVal | Number | Character
    // Number → IntConst
    // Character → CharConst
    private IRValue visitPrimaryExp(PrimaryExp primaryExp, BasicBlock insertBlock) {
        PrimaryExp.PrimaryExpOption primaryExpExtract = primaryExp.extract();
        if (primaryExpExtract instanceof PrimaryExp.PrimaryExp_Exp primaryExp_exp) {
            return this.visitExp(primaryExp_exp.exp(), insertBlock);
        } else if (primaryExpExtract instanceof PrimaryExp.PrimaryExp_LVal primaryExp_lVal) {
            IRValue lValValue = this.visitLVal(primaryExp_lVal.lVal(), insertBlock, true);
            if (IRType.isEqual(lValValue.type(), IRType.getInt8Ty())) {
                // 对于char类型，扩展后参与运算
                return this.builder.addExtendOperation(lValValue, IRType.getInt32Ty(), insertBlock);
            } else if (IRType.isEqual(lValValue.type(), IRType.getInt32Ty()) || lValValue.type() instanceof PointerType) {
                // 对于int类型或者指针类型，直接返回
                return lValValue;
            } else {
                throw new RuntimeException("When visitPrimaryExp(), got illegal type after evaluation of LVal. Got " + lValValue.type());
            }
        } else if (primaryExpExtract instanceof PrimaryExp.PrimaryExp_Number primaryExp_number) {
            return new ConstantInt(IRType.getInt32Ty(), Integer.parseInt(primaryExp_number.number().intConst().strVal()));
        } else if (primaryExpExtract instanceof PrimaryExp.PrimaryExp_Character primaryExp_character) {
            // 由于char参加运算的方式为先零拓展为int再参加运算，故此处可以直接提升为int
            return new ConstantInt(IRType.getInt32Ty(), Translator.translateCharConst(primaryExp_character.character().charConst()));
        } else {
            throw new RuntimeException("When visitPrimaryExp(), got unknown type of PrimaryExp ("
                    + primaryExpExtract.getClass().getSimpleName() + ")");
        }
    }

    // LVal → Ident ['[' Exp ']']
    private IRValue visitLVal(LVal lVal, BasicBlock insertBlock, boolean evaluation) {
        // LVal的返回结果，若做evaluation，可能的返回的类型有int, char, int*, char*
        //             若不做evaluation，可能的返回的类型有int*, char*, int(*)[], char(*)[]
        Symbol<? extends IRType> searchedSymbol = this.symbolTable.searchOrError(lVal.ident());
        if (searchedSymbol instanceof FuncSymbol) {
            throw new RuntimeException("When visitLVal(), the search result of " + lVal.ident() + " is a function");
        } else {
            // 获得左值的地址
            IRValue lValAddress;
            // 处理由于优化输出初始化等造成的Value的类型与符号表的类型不一致的问题
            if (!IRType.isEqual(searchedSymbol.type(), searchedSymbol.irValue().type())) {
                lValAddress = this.builder.addBitCastOperation(searchedSymbol.irValue(), searchedSymbol.type(), insertBlock);
            } else {
                lValAddress = searchedSymbol.irValue();
            }
            // 处理数组访问
            if (lVal.getType() == LVal.Type.ARRAY) {
                IRValue indexValue = this.visitExp(lVal.exp(), insertBlock);
                lValAddress = this.builder.addGetArrayElementPointer(lValAddress, indexValue, insertBlock);
            } else if (lVal.getType() != LVal.Type.BASIC) {
                throw new RuntimeException("When calculateLVal(), got unknown type of LVal (" + lVal.getType() + ")");
            }
            if (evaluation) {
                // 左值的地址的类型一定是pointer，此处的转换没有问题
                PointerType pointType = (PointerType) lValAddress.type();
                if (pointType.referenceType() instanceof ArrayType pointArrayType) {
                    // 数组类型做evaluation类型不同
                    return this.builder.addBitCastOperation(lValAddress, new PointerType(pointArrayType.elementType(), false), insertBlock);
                } else {
                    return this.builder.addLoadValue(lValAddress, insertBlock);
                }
            } else {
                // 不需要做左值转换
                // 注意，左值转换后是否能够用于赋值不由visitLVal负责，由处理赋值语句的函数负责
                return lValAddress;
            }
        }
    }
}
