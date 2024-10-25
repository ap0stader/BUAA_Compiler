package frontend.visitor;

import IR.IRModule;
import IR.value.BasicBlock;
import frontend.error.ErrorTable;
import frontend.lexer.Token;
import frontend.parser.CompUnit;
import frontend.parser.declaration.*;
import frontend.parser.declaration.constant.*;
import frontend.parser.declaration.function.*;
import frontend.parser.declaration.variable.*;
import frontend.parser.statement.BlockItem;
import frontend.parser.statement.Stmt;
import frontend.type.TokenType;
import frontend.visitor.symbol.*;
import util.Pair;

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
            for (Pair<VarSymbol, ArrayList<Integer>> varSymbol : this.visitGlobalVarDecl(varDecl)) {
                varSymbol.key().setIRValue(this.builder.addGlobalVariable(varSymbol.key(), varSymbol.value()));
            }
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
    private ArrayList<Pair<VarSymbol, ArrayList<Integer>>> visitGlobalVarDecl(VarDecl varDecl) {
        ArrayList<Pair<VarSymbol, ArrayList<Integer>>> ret = new ArrayList<>();
        Token bType = varDecl.typeToken();
        // visitVarDef()
        for (VarDef varDef : varDecl.varDefs()) {
            Token ident = varDef.ident();
            VarSymbol newSymbol;
            ArrayList<Integer> initVals;
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
            // 字符型截取低八位
            if (bType.type() == TokenType.CHARTK) {
                initVals.replaceAll(integer -> (int) (integer.byteValue()));
            }
            if (varDef.constExp() == null) {
                // 非数组
                if (initVals.size() != 1) {
                    throw new RuntimeException("When visitGlobalVarDecl(), initVals of identifier " + ident + " mismatch its type");
                }
                newSymbol = new VarSymbol(Translator.getVarIRType(bType, null, false), ident);
            } else {
                // 数组
                int length = this.calculator.calculateConstExp(varDef.constExp());
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
            if (this.symbolTable.insert(newSymbol)) {
                ret.add(new Pair<>(newSymbol, initVals));
            }
        }
        return ret;
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
            if (item instanceof Decl) {

            } else if (item instanceof Stmt) {

            } else {
                throw new RuntimeException("When visitFunctionBlock(), got unknown type of BlockItem (" + item.getClass().getSimpleName() + ")");
            }
        }
    }

    // Decl → ConstDecl | VarDecl
}
