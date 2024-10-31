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
import frontend.type.Symbol;
import frontend.type.TokenType;
import frontend.visitor.symbol.*;
import global.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

public class Visitor {
    private final CompUnit compUnit;
    private boolean finish = false;

    private final SymbolTable symbolTable;
    private final Calculator calculator;
    private final Builder builder;
    private final LinkedList<BasicBlock> forEndBlocks;
    private final LinkedList<BasicBlock> forTailBlocks;

    private final ErrorTable errorTable;

    private final IRModule irModule;

    public Visitor(CompUnit compUnit, ErrorTable errorTable) {
        this.compUnit = compUnit;
        this.errorTable = errorTable;
        this.symbolTable = new SymbolTable(errorTable);
        this.calculator = new Calculator(this.symbolTable);
        this.forEndBlocks = new LinkedList<>();
        this.forTailBlocks = new LinkedList<>();
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
            ArrayList<ConstSymbol> constSymbols = this.visitConstDecl(constDecl);
            for (ConstSymbol constSymbol : constSymbols) {
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
        ArrayList<ArgSymbol> funcFParams;
        if (funcDef.funcFParams() == null) {
            // 无参数
            funcFParams = new ArrayList<>();
        } else {
            // 有参数，在此处即遍历FuncFParams，先用于生成函数类型
            funcFParams = this.visitFuncFParams(funcDef.funcFParams());
        }
        // 构建函数符号
        FuncSymbol newFuncSymbol = new FuncSymbol(Translator.getFuncIRType(funcType,
                new ArrayList<>(funcFParams.stream().map(ArgSymbol::type).toList())), ident);
        // 函数尝试进入符号表，为了检查出尽可能多的错误，无论是否成功都继续
        this.symbolTable.insert(newFuncSymbol);
        // 给函数符号对应的IRValue
        newFuncSymbol.setIRValue(this.builder.addFunction(newFuncSymbol,
                new ArrayList<>(funcFParams.stream().map(ArgSymbol::argument).toList())));
        // 进入函数的作用域
        this.symbolTable.push();
        // 参数尝试进入符号表
        for (ArgSymbol argSymbol : funcFParams) {
            this.symbolTable.insert(argSymbol);
        }
        this.visitFunctionBlock(funcFParams, funcDef.block());
        // 离开函数的作用域
        this.symbolTable.pop();
    }

    // MainFuncDef → 'int' 'main' '(' ')' Block
    private void visitMainFuncDef(MainFuncDef mainFuncDef) {
        // 主函数不需要进入符号表，也没有参数
        this.builder.addMainFunction();
        // 进入主函数的作用域
        this.symbolTable.push();
        this.visitFunctionBlock(new ArrayList<>(), mainFuncDef.block());
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
            ArrayList<Integer> constInitVals;
            // 必定有初始值
            constInitVals = this.visitConstInitVal(constDef.constInitVal());
            // 字符型截取低八位
            if (bType.type() == TokenType.CHARTK) {
                constInitVals.replaceAll(integer -> (int) integer.byteValue());
            }
            if (constDef.constExp() == null) {
                // 非数组
                if (constInitVals.size() != 1) {
                    throw new RuntimeException("When visitConstDecl(), constInitVals of identifier " + ident + " mismatch its type");
                }
                newSymbol = new ConstSymbol(Translator.getConstIRType(bType, null), ident, constInitVals);
            } else {
                // 数组
                int length = this.calculator.calculateConstExp(constDef.constExp());
                if (constInitVals.size() > length) {
                    throw new RuntimeException("When visitConstDecl(), constInitVals of identifier " + ident + " is longer than its length");
                } else {
                    // 补齐未显示写出的0
                    for (int i = constInitVals.size(); i < length; i++) {
                        constInitVals.add(0);
                    }
                }
                newSymbol = new ConstSymbol(Translator.getConstIRType(bType, length), ident, constInitVals);
            }
            // 上方已经对初始值进行了分析，为了避免LLVM IR错误，判断了成功插入再添加GlobalVariable
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
            // 只有char数组能用字符串值进行初始化，此处并没有进行检查，如果给int[]初始化，按照每个字符转为int进行
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
                newSymbol = new VarSymbol(Translator.getVarIRType(bType, null), ident);
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
                newSymbol = new VarSymbol(Translator.getVarIRType(bType, length), ident);
            }
            // 上方已经对初始值进行了分析，为了避免LLVM IR错误，判断了成功插入再添加GlobalVariable
            if (this.symbolTable.insert(newSymbol)) {
                newSymbol.setIRValue(this.builder.addGlobalVariable(newSymbol, initVals));
            }
        }
    }

    // InitVal → Exp | '{' [ Exp { ',' Exp } ] '}' | StringConst
    private ArrayList<Integer> visitGlobalInitVal(InitVal initVal) {
        if (initVal.getType() == InitVal.Type.BASIC) {
            return new ArrayList<>(Collections.singletonList(this.calculator.calculateExp(initVal.exp())));
        } else if (initVal.getType() == InitVal.Type.STRING) {
            // 只有char数组能用字符串值进行初始化，此处并没有进行检查，如果给int[]初始化，按照每个字符转为int进行
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
    private ArrayList<ArgSymbol> visitFuncFParams(FuncFParams funcFParams) {
        ArrayList<ArgSymbol> ret = new ArrayList<>();
        for (FuncFParam funcFParam : funcFParams.funcFParams()) {
            if (funcFParam.getType() == FuncFParam.Type.BASIC) {
                ret.add(new ArgSymbol(Translator.getArgIRType(funcFParam.typeToken(), false), funcFParam.ident()));
            } else if (funcFParam.getType() == FuncFParam.Type.ARRAY) {
                ret.add(new ArgSymbol(Translator.getArgIRType(funcFParam.typeToken(), true), funcFParam.ident()));
            } else {
                throw new RuntimeException("When visitFuncFParams(), got unknown type of FuncFParam (" + funcFParam.getType() + ")");
            }
        }
        return ret;
    }

    // BlockItem → Decl | Stmt
    private void visitFunctionBlock(ArrayList<ArgSymbol> funcFParams, Block block) {
        // entryBlock用来进行各类的变量定义工作
        BasicBlock entryBlock = this.builder.newBasicBlock();
        this.builder.appendBasicBlock(entryBlock);
        // 给参数符号对应的IRValue
        for (ArgSymbol argSymbol : funcFParams) {
            argSymbol.setIRValue(this.builder.addArgument(argSymbol.argument(), entryBlock));
        }
        // startBlock为代码正式开始的地方
        BasicBlock startBlock = this.builder.newBasicBlock();
        BasicBlock nowBlock = startBlock;
        this.builder.appendBasicBlock(nowBlock);
        for (BlockItem blockItem : block.blockItems()) {
            if (blockItem instanceof Decl decl) {
                this.visitLocalDecl(decl, entryBlock);
            } else if (blockItem instanceof Stmt stmt) {
                nowBlock = this.visitStmt(stmt, entryBlock, nowBlock);
            } else {
                throw new RuntimeException("When visitFunctionBlock(), got unknown type of BlockItem (" + blockItem.getClass().getSimpleName() + ")");
            }
        }
        // 添加从定义块到代码开始块的跳转指令
        this.builder.addBranchInstruction(null, startBlock, null, entryBlock);
        if (block.blockItems().isEmpty() || // 没有语句
                !(block.blockItems().get(block.blockItems().size() - 1) instanceof Stmt) || // 最后一条语句不是Stmt
                (block.blockItems().get(block.blockItems().size() - 1) instanceof Stmt stmt // 最后一条语句是Stmt但是不是返回语句
                        && !(stmt.extract() instanceof Stmt.Stmt_Return))) {
            if (entryBlock.parent().type().returnType() instanceof VoidType) {
                // 无返回值的函数，补充一条返回语句
                this.builder.addReturnInstruction(null, nowBlock);
            } else if (entryBlock.parent().type().returnType() instanceof IntegerType returnIntegerType) {
                // 有返回值的函数缺少return语句（且只判断有没有 return 语句，不需要考虑 return 语句是否有返回值）
                // 也不需要检查函数体内其他的 return 语句是否有值
                // 报错，报错行号为函数结尾的’}’所在行号。强制补充一条返回0
                this.errorTable.addErrorRecord(block.rbraceToken().line(), ErrorType.MISSING_RETURN);
                this.builder.addReturnInstruction(new ConstantInt(returnIntegerType, 0), nowBlock);
            }
        }
    }

    // Decl → ConstDecl | VarDecl
    private void visitLocalDecl(Decl decl, BasicBlock entryBlock) {
        if (decl instanceof ConstDecl constDecl) {
            ArrayList<ConstSymbol> constSymbols = this.visitConstDecl(constDecl);
            for (ConstSymbol constSymbol : constSymbols) {
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
                newSymbol = new VarSymbol(Translator.getVarIRType(bType, null), ident);
                if (varDef.initVal() != null) {
                    // 有初始值
                    ArrayList<IRValue<IntegerType>> initVals;
                    if (varDef.initVal().getType() == InitVal.Type.BASIC) {
                        // CAST 并非函数调用处，SysY保证Exp经过evaluation的类型为IntegerType
                        initVals = new ArrayList<>(Collections.singletonList(
                                IRValue.cast(this.visitExp(varDef.initVal().exp(), entryBlock))));
                    } else {
                        throw new RuntimeException("When visitLocalVarDecl(), initVals of identifier " + ident + " mismatch its type. " +
                                "Got " + varDef.initVal().getType() + ", expected " + InitVal.Type.BASIC);
                    }
                    newSymbol.setIRValue(this.builder.addLocalVariable(newSymbol, initVals, entryBlock));
                } else {
                    newSymbol.setIRValue(this.builder.addLocalVariable(newSymbol, null, entryBlock));
                }
            } else {
                Integer length = this.calculator.calculateConstExp(varDef.constExp());
                newSymbol = new VarSymbol(Translator.getVarIRType(bType, length), ident);
                // 数组
                if (varDef.initVal() != null) {
                    ArrayList<IRValue<IntegerType>> initVals;
                    // 有初始值
                    if (varDef.initVal().getType() == InitVal.Type.ARRAY) {
                        // CAST 并非函数调用处，SysY保证Exp经过evaluation的类型为IntegerType
                        initVals = new ArrayList<>(varDef.initVal().exps().stream()
                                .map((exp -> IRValue.<IntegerType>cast(this.visitExp(exp, entryBlock)))).toList());
                    } else if (varDef.initVal().getType() == InitVal.Type.STRING) {
                        // 只有char数组能用字符串值进行初始化，此处强制指定类型为char，如果给int[]初始化，会出现CastInst
                        initVals = new ArrayList<>(Translator.translateStringConst(varDef.initVal().stringConst()).stream()
                                .map((character) -> new ConstantInt(IRType.getInt8Ty(), character)).toList());
                    } else {
                        throw new RuntimeException("When visitLocalVarDecl(), initVals of identifier " + ident + " mismatch its type. " +
                                "Got " + varDef.initVal().getType() + ", expected " + InitVal.Type.ARRAY + "/" + InitVal.Type.STRING);
                    }
                    if (initVals.size() > length) {
                        throw new RuntimeException("When visitLocalVarDecl(), initVals of identifier " + ident + " is longer than its length");
                    } else {
                        // 补齐未显示写出的0
                        IntegerType initValType = bType.type() == TokenType.CHARTK ? IRType.getInt8Ty() : IRType.getInt32Ty();
                        for (int i = initVals.size(); i < length; i++) {
                            initVals.add(new ConstantInt(initValType, 0));
                        }
                    }
                    newSymbol.setIRValue(this.builder.addLocalVariable(newSymbol, initVals, entryBlock));
                } else {
                    newSymbol.setIRValue(this.builder.addLocalVariable(newSymbol, null, entryBlock));
                }
            }
            // 即便插入不成功，生成了alloca指令也不会导致LLVM IR错误
            this.symbolTable.insert(newSymbol);
        }
    }

    // Exp → AddExp
    private IRValue<?> visitExp(Exp exp, BasicBlock insertBlock) {
        return this.visitAddExp(exp.addExp(), insertBlock);
    }

    // AddExp → MulExp | AddExp ('+' | '−') MulExp
    private IRValue<?> visitAddExp(AddExp addExp, BasicBlock insertBlock) {
        IRValue<?> resultValue = this.visitMulExp(addExp.mulExps().get(0), insertBlock);
        if (addExp.symbols().isEmpty()) {
            return resultValue;
        } else {
            // 有多个MulExp
            // CAST SysY语法保证多个MulExp的类型为IntegerType，SysY中不存在指针类型的加减的（使用GetElementPtr）
            IRValue<IntegerType> integerResultValue = IRValue.cast(resultValue);
            for (int i = 0; i < addExp.symbols().size(); i++) {
                IRValue<IntegerType> newValue = IRValue.cast(this.visitMulExp(addExp.mulExps().get(i + 1), insertBlock));
                integerResultValue = this.builder.addBinaryOperation(addExp.symbols().get(i), integerResultValue, newValue, insertBlock);
            }
            return integerResultValue;
        }
    }

    // MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    private IRValue<?> visitMulExp(MulExp mulExp, BasicBlock insertBlock) {
        IRValue<?> resultValue = this.visitUnaryExp(mulExp.unaryExps().get(0), insertBlock);
        if (mulExp.symbols().isEmpty()) {
            return resultValue;
        } else {
            // 有多个UnaryExp
            // CAST SysY语法保证多个MulExp的类型为IntegerType
            IRValue<IntegerType> integerResultValue = IRValue.cast(resultValue);
            for (int i = 0; i < mulExp.symbols().size(); i++) {
                IRValue<IntegerType> newValue = IRValue.cast(this.visitUnaryExp(mulExp.unaryExps().get(i + 1), insertBlock));
                integerResultValue = this.builder.addBinaryOperation(mulExp.symbols().get(i), integerResultValue, newValue, insertBlock);
            }
            return integerResultValue;
        }
    }

    // UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    // UnaryOp → '+' | '−' | '!'
    private IRValue<?> visitUnaryExp(UnaryExp unaryExp, BasicBlock insertBlock) {
        UnaryExp.UnaryExpOption unaryExpExtract = unaryExp.extract();
        if (unaryExpExtract instanceof UnaryExp.UnaryExp_UnaryOp unaryExp_unaryOp) {
            if (unaryExp_unaryOp.unaryOp().symbol().type() == TokenType.PLUS) {
                // 对于+直接提取内部值，但是对于不可运算的值此处不识别
                // CAST 并非函数调用处，SysY保证UnaryExp经过evaluation的类型为IntegerType
                return IRValue.<IntegerType>cast(this.visitUnaryExp(unaryExp_unaryOp.unaryExp(), insertBlock));
            } else if (unaryExp_unaryOp.unaryOp().symbol().type() == TokenType.MINU) {
                // CAST 并非函数调用处，SysY保证UnaryExp经过evaluation的类型为IntegerType
                IRValue<IntegerType> subValue = IRValue.cast(this.visitUnaryExp(unaryExp_unaryOp.unaryExp(), insertBlock));
                return this.builder.addBinaryOperation(unaryExp_unaryOp.unaryOp().symbol(), ConstantInt.ZERO_I32(), subValue, insertBlock);
            } else if (unaryExp_unaryOp.unaryOp().symbol().type() == TokenType.NOT) {
                // 仅在条件表达式中有可能出现
                // CAST 并非函数调用处，SysY保证UnaryExp经过evaluation的类型为IntegerType
                IRValue<IntegerType> notValue = IRValue.cast(this.visitUnaryExp(unaryExp_unaryOp.unaryExp(), insertBlock));
                return this.builder.addIcmpOperation(unaryExp_unaryOp.unaryOp().symbol(), notValue, null, insertBlock);
            } else {
                throw new RuntimeException("When visitUnaryExp(), got unexpected symbol " + unaryExp_unaryOp.unaryOp().symbol()
                        + ", expected " + TokenType.PLUS + "/" + TokenType.MINU);
            }
        } else if (unaryExpExtract instanceof UnaryExp.UnaryExp_PrimaryExp unaryExp_primaryExp) {
            return this.visitPrimaryExp(unaryExp_primaryExp.primaryExp(), insertBlock);
        } else if (unaryExpExtract instanceof UnaryExp.UnaryExp_IdentFuncCall unaryExp_identFuncCall) {
            Symbol<?, ?> searchedSymbol = this.symbolTable.searchOrError(unaryExp_identFuncCall.ident());
            if (searchedSymbol instanceof FuncSymbol funcSymbol) {
                ArrayList<IRValue<?>> funcRParamsValues = this.visitFuncRParams(unaryExp_identFuncCall.funcRParams(),
                        unaryExp_identFuncCall.ident(), funcSymbol, insertBlock);
                // 检查函数的参数的数量和类型在visitFuncRParams中
                return this.builder.addCallFunction(funcSymbol.irValue(), funcRParamsValues, insertBlock);
            } else {
                // 查找不到符号，或者符号不是函数，强制置为0
                return ConstantInt.ZERO_I32();
            }
        } else {
            throw new RuntimeException("When visitUnaryExp(), got unknown type of UnaryExp ("
                    + unaryExpExtract.getClass().getSimpleName() + ")");
        }
    }

    // FuncRParams → Exp { ',' Exp }
    private ArrayList<IRValue<?>> visitFuncRParams(FuncRParams funcRParams,
                                                   Token indentFuncCall, FuncSymbol funcSymbol, BasicBlock insertBlock) {
        // 将函数的参数解析为对应IRValue，同时检查函数的参数类型是否合法
        ArrayList<IRType> parametersType = funcSymbol.type().parametersType();
        // 需要判断是否有funcRParams
        ArrayList<IRValue<?>> funcRParamsValues;
        if (funcRParams == null) {
            // 无参数
            funcRParamsValues = new ArrayList<>();
        } else {
            // 有参数
            funcRParamsValues = new ArrayList<>(funcRParams.exps().stream().map((exp) -> this.visitExp(exp, insertBlock)).toList());
        }
        // 一行只有一个错误，不重复报错
        if (parametersType.size() != funcRParamsValues.size()) {
            // 函数的参数数量不对应，不做对应检查，直接报错并返回结果
            this.errorTable.addErrorRecord(indentFuncCall.line(), ErrorType.FUNCRPARAMS_NUM_MISMATCH,
                    "The function call " + indentFuncCall + " has a wrong parameters number. " +
                            "Got " + funcRParamsValues.size() + ", expected " + parametersType.size());
        } else {
            // 将实参翻译为形参需要的类型
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
                        // CAST 上方的instanceof确保转换正确
                        funcRParamsValues.set(i, this.builder.addExtendOperation(IRValue.cast(funcRParamsValues.get(i)), fIntegerType, insertBlock));
                    } else if (rIntegerType.size() > fIntegerType.size()) {
                        // 实参大于形参，截断
                        // CAST 上方的instanceof确保转换正确
                        funcRParamsValues.set(i, this.builder.addTruncOperation(IRValue.cast(funcRParamsValues.get(i)), fIntegerType, insertBlock));
                    }
                }
            }
        }
        return funcRParamsValues;
    }

    // PrimaryExp → '(' Exp ')' | LVal | Number | Character
    // Number → IntConst
    // Character → CharConst
    private IRValue<?> visitPrimaryExp(PrimaryExp primaryExp, BasicBlock insertBlock) {
        PrimaryExp.PrimaryExpOption primaryExpExtract = primaryExp.extract();
        if (primaryExpExtract instanceof PrimaryExp.PrimaryExp_Exp primaryExp_exp) {
            return this.visitExp(primaryExp_exp.exp(), insertBlock);
        } else if (primaryExpExtract instanceof PrimaryExp.PrimaryExp_LVal primaryExp_lVal) {
            return this.visitLValEvaluation(primaryExp_lVal.lVal(), insertBlock);
        } else if (primaryExpExtract instanceof PrimaryExp.PrimaryExp_Number primaryExp_number) {
            return new ConstantInt(IRType.getInt32Ty(), Integer.parseInt(primaryExp_number.number().intConst().strVal()));
        } else if (primaryExpExtract instanceof PrimaryExp.PrimaryExp_Character primaryExp_character) {
            // 根据C语言的标准，字符的类型为int
            return new ConstantInt(IRType.getInt32Ty(), Translator.translateCharConst(primaryExp_character.character().charConst()));
        } else {
            throw new RuntimeException("When visitPrimaryExp(), got unknown type of PrimaryExp ("
                    + primaryExpExtract.getClass().getSimpleName() + ")");
        }
    }

    // LVal → Ident ['[' Exp ']']
    private IRValue<?> visitLValEvaluation(LVal lVal, BasicBlock insertBlock) {
        // LVal做evaluation，可能的返回的类型有int, char, int*, char*
        Symbol<?, ?> searchedSymbol = this.symbolTable.searchOrError(lVal.ident());
        if (searchedSymbol instanceof FuncSymbol) {
            throw new RuntimeException("When visitLValEvaluation(), the search result of " + lVal.ident() + " is a function");
        } else if (searchedSymbol instanceof ConstSymbol || searchedSymbol instanceof VarSymbol || searchedSymbol instanceof ArgSymbol) {
            // CAST 上方的instanceof确保转换正确
            IRValue<PointerType> lValAddress = IRValue.cast(searchedSymbol.irValue());
            // 处理由于优化初始化造成的符号对应的Value指向的类型与符号的登记类型不一致的问题
            if (!IRType.isEqual(searchedSymbol.type(), lValAddress.type().referenceType())) {
                lValAddress = this.builder.addBitCastOperation(searchedSymbol.irValue(), new PointerType(searchedSymbol.type()), insertBlock);
            }
            if (lVal.getType() == LVal.Type.BASIC && lValAddress.type().referenceType() instanceof IntegerType) {
                // 变量、常量
                return this.builder.loadLVal(lValAddress, insertBlock);
            } else if (lVal.getType() == LVal.Type.BASIC &&
                    (lValAddress.type().referenceType() instanceof ArrayType || lValAddress.type().referenceType() instanceof PointerType)) {
                // 直接evaluation数组名，将会导致decay
                // ArrayType是常量和变量的，PointerType是参数的
                return this.builder.addGetArrayElementPointer(lValAddress, ConstantInt.ZERO_I32(), insertBlock);
            } else if (lVal.getType() == LVal.Type.ARRAY &&
                    (lValAddress.type().referenceType() instanceof ArrayType || lValAddress.type().referenceType() instanceof PointerType)) {
                // 数组
                // ArrayType是常量和变量的，PointerType是参数的
                // CAST 并非函数调用处，SysY保证Exp经过evaluation的类型为IntegerType
                IRValue<IntegerType> indexValue = IRValue.cast(this.visitExp(lVal.exp(), insertBlock));
                lValAddress = this.builder.addGetArrayElementPointer(lValAddress, indexValue, insertBlock);
                return this.builder.loadLVal(lValAddress, insertBlock);
            } else {
                throw new RuntimeException("When visitLValEvaluation(), illegal type of searchedSymbol (" + searchedSymbol.irValue().type() +
                        ") and LVal (" + lVal.getType() + ")");
            }
        } else if (searchedSymbol == null) {
            // 查找不到符号，强制置为0
            return ConstantInt.ZERO_I32();
        } else {
            throw new RuntimeException("When visitLValEvaluation(), got unknown type of searchedSymbol (" + searchedSymbol.getClass().getSimpleName() + ")");
        }
    }

    // Stmt
    private BasicBlock visitStmt(Stmt stmt, BasicBlock entryBlock, BasicBlock nowBlock) {
        Stmt.StmtOption stmtOption = stmt.extract();
        if (stmtOption instanceof Stmt.Stmt_Semicn) {
            // Stmt → ';'
            return nowBlock;
        } else if (stmtOption instanceof Stmt.Stmt_Exp stmt_exp) {
            // Stmt → Exp ';'
            this.visitExp(stmt_exp.exp(), nowBlock);
        } else if (stmtOption instanceof Stmt.Stmt_Return stmt_return) {
            this.visitStmtReturn(stmt_return, nowBlock);
        } else if (stmtOption instanceof Stmt.Stmt_LValAssign stmt_lValAssign) {
            this.visitStmtLValAssign(stmt_lValAssign, nowBlock);
        } else if (stmtOption instanceof Stmt.Stmt_LValGetint stmt_lValGetint) {
            this.visitStmtLValGetInt(stmt_lValGetint, nowBlock);
        } else if (stmtOption instanceof Stmt.Stmt_LValGetchar stmt_lValGetchar) {
            this.visitStmtLValGetChar(stmt_lValGetchar, nowBlock);
        } else if (stmtOption instanceof Stmt.Stmt_Printf stmt_printf) {
            this.visitStmtPrintf(stmt_printf, nowBlock);
        } else if (stmtOption instanceof Stmt.Stmt_Break stmt_break) {
            // Stmt → 'break' ';'
            if (this.forEndBlocks.isEmpty()) {
                this.errorTable.addErrorRecord(stmt_break.breakToken().line(), ErrorType.BREAK_CONTINUE_OUTSIDE_LOOP);
            } else {
                this.builder.addBranchInstruction(null, this.forEndBlocks.peek(), null, nowBlock);
            }
        } else if (stmtOption instanceof Stmt.Stmt_Continue stmt_continue) {
            // Stmt → 'continue' ';'
            if (this.forTailBlocks.isEmpty()) {
                this.errorTable.addErrorRecord(stmt_continue.continueToken().line(), ErrorType.BREAK_CONTINUE_OUTSIDE_LOOP);
            } else {
                this.builder.addBranchInstruction(null, this.forTailBlocks.peek(), null, nowBlock);
            }
        } else if (stmtOption instanceof Stmt.Stmt_Block stmt_block) {
            return this.visitStmtBlock(stmt_block.block(), entryBlock, nowBlock);
        } else if (stmtOption instanceof Stmt.Stmt_If stmt_if) {
            return this.visitStmtIf(stmt_if, entryBlock, nowBlock);
        } else if (stmtOption instanceof Stmt.Stmt_For stmt_for) {
            return this.visitStmtFor(stmt_for, entryBlock, nowBlock);
        } else {
            if (Config.visitorThrowable) {
                throw new RuntimeException("When visitStmt(), got unknown type of Stmt (" + stmt.getClass().getSimpleName() + ")");
            } else {
                return nowBlock;
            }
        }
        return nowBlock;
    }

    // Stmt → Block
    private BasicBlock visitStmtBlock(Block block, BasicBlock entryBlock, BasicBlock nowBlock) {
        this.symbolTable.push();
        for (BlockItem blockItem : block.blockItems()) {
            if (blockItem instanceof Decl decl) {
                this.visitLocalDecl(decl, entryBlock);
            } else if (blockItem instanceof Stmt stmt) {
                nowBlock = this.visitStmt(stmt, entryBlock, nowBlock);
            } else {
                throw new RuntimeException("When visitFunctionBlock(), got unknown type of BlockItem (" + blockItem.getClass().getSimpleName() + ")");
            }
        }
        this.symbolTable.pop();
        return nowBlock;
    }

    // Stmt → 'return' [Exp] ';'
    private void visitStmtReturn(Stmt.Stmt_Return stmt_return, BasicBlock nowBlock) {
        if (nowBlock.parent().type().returnType() instanceof VoidType) {
            if (stmt_return.exp() != null) {
                this.errorTable.addErrorRecord(stmt_return.returnToken().line(), ErrorType.RETURN_TYPE_MISMATCH);
                // MAYBE 由于不存在恶意换行，此处不分析Exp的内容
            }
            // 无返回值函数无论是否给定Exp，都返回void
            this.builder.addReturnInstruction(null, nowBlock);
        } else if (nowBlock.parent().type().returnType() instanceof IntegerType returnIntegerType) {
            if (stmt_return.exp() == null) {
                // 有返回值函数如果没有给定Exp，强制置为0
                this.builder.addReturnInstruction(new ConstantInt(returnIntegerType, 0), nowBlock);
            } else {
                // CAST 并非函数调用处，SysY保证Exp经过evaluation的类型为IntegerType
                IRValue<IntegerType> returnValue = IRValue.cast(this.visitExp(stmt_return.exp(), nowBlock));
                this.builder.addReturnInstruction(returnValue, nowBlock);
            }
        }
    }

    // Stmt → LVal '=' Exp ';'
    private void visitStmtLValAssign(Stmt.Stmt_LValAssign stmt_lValAssign, BasicBlock nowBlock) {
        // CAST 并非函数调用处，SysY保证Exp经过evaluation的类型为IntegerType
        IRValue<IntegerType> expValue = IRValue.cast(this.visitExp(stmt_lValAssign.exp(), nowBlock));
        this.builder.storeLVal(expValue, this.visitLValAddress(stmt_lValAssign.lVal(), nowBlock), nowBlock);
    }

    // Stmt → LVal '=' 'getint' '(' ')' ';'
    private void visitStmtLValGetInt(Stmt.Stmt_LValGetint stmt_lValGetint, BasicBlock nowBlock) {
        // CAST 库函数定义保证了正确性
        IRValue<IntegerType> getintValue = IRValue.cast(this.builder.addCallLibFunction("getint", new ArrayList<>(), nowBlock));
        this.builder.storeLVal(getintValue, this.visitLValAddress(stmt_lValGetint.lVal(), nowBlock), nowBlock);
    }

    // Stmt → LVal '=' 'getchar' '(' ')' ';'
    private void visitStmtLValGetChar(Stmt.Stmt_LValGetchar stmt_lValGetchar, BasicBlock nowBlock) {
        // CAST 库函数定义保证了正确性
        IRValue<IntegerType> getcharValue = IRValue.cast(this.builder.addCallLibFunction("getchar", new ArrayList<>(), nowBlock));
        this.builder.storeLVal(getcharValue, this.visitLValAddress(stmt_lValGetchar.lVal(), nowBlock), nowBlock);
    }

    // LVal → Ident ['[' Exp ']']
    private IRValue<PointerType> visitLValAddress(LVal lVal, BasicBlock insertBlock) {
        // LVal做evaluation，可能的返回的类型有int, char, int*, char*
        Symbol<?, ?> searchedSymbol = this.symbolTable.searchOrError(lVal.ident());
        if (searchedSymbol instanceof FuncSymbol) {
            throw new RuntimeException("When visitLValEvaluation(), the search result of " + lVal.ident() + " is a function");
        } else if (searchedSymbol instanceof VarSymbol || searchedSymbol instanceof ArgSymbol) {
            // CAST 上方的instanceof确保转换正确
            IRValue<PointerType> lValAddress = IRValue.cast(searchedSymbol.irValue());
            // 处理由于优化初始化造成的符号对应的Value指向的类型与符号的登记类型不一致的问题
            if (!IRType.isEqual(searchedSymbol.type(), lValAddress.type().referenceType())) {
                lValAddress = this.builder.addBitCastOperation(searchedSymbol.irValue(), new PointerType(searchedSymbol.type()), insertBlock);
            }
            if (lVal.getType() == LVal.Type.BASIC && lValAddress.type().referenceType() instanceof IntegerType) {
                // 变量、常量
                return lValAddress;
            } else if (lVal.getType() == LVal.Type.ARRAY &&
                    (lValAddress.type().referenceType() instanceof ArrayType || lValAddress.type().referenceType() instanceof PointerType)) {
                // 数组
                // ArrayType是常量和变量的，PointerType是参数的
                // CAST 并非函数调用处，SysY保证Exp经过evaluation的类型为IntegerType
                IRValue<IntegerType> indexValue = IRValue.cast(this.visitExp(lVal.exp(), insertBlock));
                return this.builder.addGetArrayElementPointer(lValAddress, indexValue, insertBlock);
            } else {
                throw new RuntimeException("When visitLValEvaluation(), illegal type of searchedSymbol (" + searchedSymbol.irValue().type() +
                        ") and LVal (" + lVal.getType() + ")");
            }
        } else if (searchedSymbol instanceof ConstSymbol) {
            // 查找到的符号为常量，返回null，配合Builder中的addStoreLVal为null时不生成StoreInst
            this.errorTable.addErrorRecord(lVal.ident().line(), ErrorType.TRY_MODIFY_CONST,
                    "Try modify a const '" + searchedSymbol.name() + "' defined at line " + searchedSymbol.line());
            // MAYBE 由于不存在恶意换行，此处不分析数组下标的内容
            return null;
        } else if (searchedSymbol == null) {
            // 查找不到符号，返回null，配合Builder中的addStoreLVal为null时不生成StoreInst
            return null;
        } else {
            throw new RuntimeException("When visitLValEvaluation(), got unknown type of searchedSymbol (" + searchedSymbol.getClass().getSimpleName() + ")");
        }
    }

    // Stmt → 'printf' '(' StringConst { ',' Exp} ')' ';'
    private void visitStmtPrintf(Stmt.Stmt_Printf stmt_printf, BasicBlock nowBlock) {
        ArrayList<Integer> formatStringChar = Translator.translateStringConst(stmt_printf.stringConst());
        ArrayList<Integer> bufferStringChar = new ArrayList<>();
        int expIndex = 0;
        for (int i = 0; i < formatStringChar.size(); i++) {
            if (formatStringChar.get(i) != '%') {
                bufferStringChar.add(formatStringChar.get(i));
            } else {
                if (formatStringChar.get(i + 1) == 'c' || formatStringChar.get(i + 1) == 'd') {
                    // 跳过格式控制符
                    i++;
                    if (!bufferStringChar.isEmpty()) {
                        bufferStringChar.add(0);
                        this.builder.addCallLibFunction("putstr",
                                new ArrayList<>(Collections.singletonList(this.builder.loadConstStringPointer(bufferStringChar, nowBlock))),
                                nowBlock);
                        bufferStringChar.clear();
                    }
                    IRValue<IntegerType> printValue;
                    if (expIndex >= stmt_printf.exps().size()) {
                        // 每行只报一个错误由errorTable保证
                        this.errorTable.addErrorRecord(stmt_printf.printfToken().line(), ErrorType.PRINTF_RPARAMS_NUM_MISMATCH);
                        printValue = ConstantInt.ZERO_I32();
                    } else {
                        // CAST 并非函数调用处，SysY保证Exp经过evaluation的类型为IntegerType
                        printValue = IRValue.cast(this.visitExp(stmt_printf.exps().get(expIndex), nowBlock));
                    }
                    expIndex++;
                    if (printValue.type().size() < IRType.getInt32Ty().size()) {
                        printValue = this.builder.addExtendOperation(printValue, IRType.getInt32Ty(), nowBlock);
                    }
                    if (formatStringChar.get(i) == 'c') {
                        this.builder.addCallLibFunction("putch", new ArrayList<>(Collections.singletonList(printValue)), nowBlock);
                    } else { // formatStringChar.get(i) == 'd'
                        this.builder.addCallLibFunction("putint", new ArrayList<>(Collections.singletonList(printValue)), nowBlock);
                    }
                } else {
                    bufferStringChar.add(formatStringChar.get(i));
                }
            }
        }
        if (bufferStringChar.size() > 1) {
            // Translator会自动补充一个0在结尾
            this.builder.addCallLibFunction("putstr",
                    new ArrayList<>(Collections.singletonList(this.builder.loadConstStringPointer(bufferStringChar, nowBlock))),
                    nowBlock);
        }
        // 每行只报一个错误由errorTable保证
        if (expIndex < stmt_printf.exps().size()) {
            this.errorTable.addErrorRecord(stmt_printf.printfToken().line(), ErrorType.PRINTF_RPARAMS_NUM_MISMATCH);
        }
        // MAYBE 由于不存在恶意换行，此处不分析剩余Exp的内容
    }

    // Stmt → 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    private BasicBlock visitStmtIf(Stmt.Stmt_If stmt_if, BasicBlock entryBlock, BasicBlock nowBlock) {
        BasicBlock ifBodyBlock = this.builder.newBasicBlock();
        BasicBlock ifEndBlock = this.builder.newBasicBlock();
        if (stmt_if.elseStmt() == null) {
            // 无else语句
            this.visitCond(stmt_if.cond(), ifBodyBlock, ifEndBlock, nowBlock);
            // if语句体
            this.builder.appendBasicBlock(ifBodyBlock);
            BasicBlock ifBodyLastBlock = this.visitStmt(stmt_if.ifStmt(), entryBlock, ifBodyBlock);
            this.builder.addBranchInstruction(null, ifEndBlock, null, ifBodyLastBlock);
        } else {
            // 有else语句
            BasicBlock ifElseBlock = this.builder.newBasicBlock();
            this.visitCond(stmt_if.cond(), ifBodyBlock, ifElseBlock, nowBlock);
            // if语句体
            this.builder.appendBasicBlock(ifBodyBlock);
            BasicBlock ifBodyLastBlock = this.visitStmt(stmt_if.ifStmt(), entryBlock, ifBodyBlock);
            this.builder.addBranchInstruction(null, ifEndBlock, null, ifBodyLastBlock);
            // else语句体
            this.builder.appendBasicBlock(ifElseBlock);
            BasicBlock ifElseLastBlock = this.visitStmt(stmt_if.elseStmt(), entryBlock, ifElseBlock);
            this.builder.addBranchInstruction(null, ifEndBlock, null, ifElseLastBlock);
        }
        // if语句后
        this.builder.appendBasicBlock(ifEndBlock);
        return ifEndBlock;
    }

    // Cond → LOrExp
    private void visitCond(Cond cond, BasicBlock trueBlock, BasicBlock falseBlock, BasicBlock insertBlock) {
        this.visitLOrExp(cond.lOrExp(), trueBlock, falseBlock, insertBlock);
    }

    // LOrExp → LAndExp | LOrExp '||' LAndExp
    private void visitLOrExp(LOrExp lOrExp, BasicBlock trueBlock, BasicBlock falseBlock, BasicBlock insertBlock) {
        for (int i = 0; i < lOrExp.lAndExps().size(); i++) {
            if (i == lOrExp.lAndExps().size() - 1) {
                // 最后一个LAndExp，为假时要跳转到falseBlock
                this.visitLAndExp(lOrExp.lAndExps().get(i), trueBlock, falseBlock, insertBlock);
            } else {
                // 不是最后一个LAndExp，为假时跳转到下一个判断条件所在的block
                // 真则直接进入trueBlock，实现短路求值
                BasicBlock nextBlock = this.builder.newBasicBlock();
                this.visitLAndExp(lOrExp.lAndExps().get(i), trueBlock, nextBlock, insertBlock);
                // 进入新的BasicBlock，判断下一个LAndExp
                this.builder.appendBasicBlock(nextBlock);
                insertBlock = nextBlock;
            }
        }
    }

    // LAndExp → EqExp | LAndExp '&&' EqExp
    private void visitLAndExp(LAndExp lAndExp, BasicBlock trueBlock, BasicBlock falseBlock, BasicBlock insertBlock) {
        for (int i = 0; i < lAndExp.eqExps().size(); i++) {
            IRValue<IntegerType> icmpResult = this.visitEqExp(lAndExp.eqExps().get(i), insertBlock);
            if (i == lAndExp.eqExps().size() - 1) {
                // 最后一个EqExp，为真时要跳转到trueBlock
                this.builder.addBranchInstruction(icmpResult, trueBlock, falseBlock, insertBlock);
            } else {
                // 不是最后一个EqExp，为真时跳转到下一个判断条件所在的block
                // 假则直接进入falseBlock，实现短路求值
                BasicBlock nextBlock = this.builder.newBasicBlock();
                this.builder.addBranchInstruction(icmpResult, nextBlock, falseBlock, insertBlock);
                // 进入新的BasicBlock，判断下一个EqExp
                this.builder.appendBasicBlock(nextBlock);
                insertBlock = nextBlock;
            }
        }
    }

    // EqExp → RelExp | EqExp ('==' | '!=') RelExp
    private IRValue<IntegerType> visitEqExp(EqExp eqExp, BasicBlock insertBlock) {
        IRValue<IntegerType> resultValue = this.visitRelExp(eqExp.relExps().get(0), insertBlock);
        // 有多个RelExp
        for (int i = 0; i < eqExp.symbols().size(); i++) {
            IRValue<IntegerType> newValue = IRValue.cast(this.visitRelExp(eqExp.relExps().get(i + 1), insertBlock));
            resultValue = this.builder.addIcmpOperation(eqExp.symbols().get(i), resultValue, newValue, insertBlock);
        }
        if (IRType.isEqual(resultValue.type(), IRType.getInt1Ty())) {
            return resultValue;
        } else {
            // 如果EqExp层和RelExp层都没有做过比较那么在离开EqExp层需要做一次比较
            return this.builder.addIcmpOperation(null, resultValue, null, insertBlock);
        }
    }

    // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    private IRValue<IntegerType> visitRelExp(RelExp relExp, BasicBlock insertBlock) {
        // CAST 并非函数调用处，SysY保证AddExp经过evaluation的类型为IntegerType
        IRValue<IntegerType> resultValue = IRValue.cast(this.visitAddExp(relExp.addExps().get(0), insertBlock));
        // 可能有多个AddExp
        for (int i = 0; i < relExp.symbols().size(); i++) {
            // CAST SysY语法保证多个AddExp的类型为IntegerType
            IRValue<IntegerType> newValue = IRValue.cast(this.visitAddExp(relExp.addExps().get(i + 1), insertBlock));
            resultValue = this.builder.addIcmpOperation(relExp.symbols().get(i), resultValue, newValue, insertBlock);
        }
        return resultValue;
    }

    // Stmt → 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    private BasicBlock visitStmtFor(Stmt.Stmt_For stmt_for, BasicBlock entryBlock, BasicBlock nowBlock) {
        if (stmt_for.initForStmt() != null) {
            this.visitForStmt(stmt_for.initForStmt(), nowBlock);
        }
        BasicBlock forBodyBlock = this.builder.newBasicBlock();
        BasicBlock forEndBlock = this.builder.newBasicBlock();
        BasicBlock forHeadBlock;
        if (stmt_for.cond() == null) {
            // 无判断条件，直接进入循环体
            this.builder.addBranchInstruction(null, forBodyBlock, null, nowBlock);
            forHeadBlock = forBodyBlock;
        } else {
            // 有判断条件，进入判断条件体
            BasicBlock forCondBlock = this.builder.newBasicBlock();
            this.builder.addBranchInstruction(null, forCondBlock, null, nowBlock);
            this.visitCond(stmt_for.cond(), forBodyBlock, forEndBlock, forCondBlock);
            this.builder.appendBasicBlock(forCondBlock);
            forHeadBlock = forCondBlock;
        }
        this.builder.appendBasicBlock(forBodyBlock);
        if (stmt_for.tailForStmt() == null) {
            // 无结尾ForStmt，直接回到头
            this.forTailBlocks.push(forHeadBlock);
            this.forEndBlocks.push(forEndBlock);
            BasicBlock forBodyLastBlock = this.visitStmt(stmt_for.stmt(), entryBlock, forBodyBlock);
            this.builder.addBranchInstruction(null, forHeadBlock, null, forBodyLastBlock);
        } else {
            // 有结尾ForStmt，进入结尾ForStmt
            BasicBlock forTailBlock = this.builder.newBasicBlock();
            this.visitForStmt(stmt_for.tailForStmt(), forTailBlock);
            this.builder.addBranchInstruction(null, forHeadBlock, null, forTailBlock);
            this.forTailBlocks.push(forTailBlock);
            this.forEndBlocks.push(forEndBlock);
            BasicBlock forBodyLastBlock = this.visitStmt(stmt_for.stmt(), entryBlock, forBodyBlock);
            this.builder.addBranchInstruction(null, forTailBlock, null, forBodyLastBlock);
            this.builder.appendBasicBlock(forTailBlock);
        }
        // for语句后
        this.builder.appendBasicBlock(forEndBlock);
        this.forTailBlocks.pop();
        this.forEndBlocks.pop();
        return forEndBlock;
    }

    // ForStmt → LVal '=' Exp
    private void visitForStmt(ForStmt forStmt, BasicBlock nowBlock) {
        // CAST 并非函数调用处，SysY保证Exp经过evaluation的类型为IntegerType
        IRValue<IntegerType> expValue = IRValue.cast(this.visitExp(forStmt.exp(), nowBlock));
        this.builder.storeLVal(expValue, this.visitLValAddress(forStmt.lVal(), nowBlock), nowBlock);
    }
}
