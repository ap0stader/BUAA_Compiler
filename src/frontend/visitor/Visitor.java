package frontend.visitor;

import IR.IRModule;
import frontend.error.ErrorTable;
import frontend.lexer.Token;
import frontend.parser.CompUnit;
import frontend.parser.declaration.*;
import frontend.parser.declaration.constant.*;
import frontend.parser.declaration.function.*;
import frontend.parser.declaration.variable.*;
import frontend.parser.expression.*;
import frontend.type.TokenType;
import frontend.visitor.symbol.*;
import util.Pair;

import java.util.ArrayList;

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
        // 全局符号表
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
            for (ConstSymbol globalConstant : this.visitConstDecl(constDecl)) {
                globalConstant.setIRValue(this.builder.addGlobalConstant(globalConstant));
            }
        } else if (decl instanceof VarDecl varDecl) {
            for (Pair<VarSymbol, ArrayList<Integer>> globalVariable : this.visitGlobalVarDecl(varDecl)) {
                globalVariable.key().setIRValue(this.builder.addGlobalVariable(globalVariable.key(), globalVariable.value()));
            }
        }
    }

    private void visitFuncDef(FuncDef funcDef) {

    }

    private void visitMainFuncDef(MainFuncDef mainFuncDef) {

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
            ArrayList<Integer> initVals;
            ConstSymbol newSymbol;
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
            ArrayList<Integer> initVals = new ArrayList<>();
            initVals.add(this.calculator.calculateConstExp(constInitVal.constExp()));
            return initVals;
        } else if (constInitVal.getType() == ConstInitVal.Type.STRING) {
            return Translator.translateStringConst(constInitVal.stringConst());
        } else if (constInitVal.getType() == ConstInitVal.Type.ARRAY) {
            ArrayList<Integer> initVals = new ArrayList<>();
            for (ConstExp constExp : constInitVal.constExps()) {
                initVals.add(this.calculator.calculateConstExp(constExp));
            }
            return initVals;
        } else {
            throw new RuntimeException("When visitConstInitVal(), got unknown type of ConstInitVal ("
                    + constInitVal.getType() + ")");
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
            ArrayList<Integer> initVals;
            VarSymbol newSymbol;
            if (varDef.initVal() == null) {
                // 无初始值
                initVals = new ArrayList<>();
                if (varDef.constExp() == null) {
                    // 不是数组的给一个0，是数组的直接利用补齐0，因为数组的长度可以等于0，所以不能直接所有都给一个0
                    initVals.add(0);
                }
            } else {
                // 有初始值
                initVals = this.visitInitValAsConst(varDef.initVal());
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
    private ArrayList<Integer> visitInitValAsConst(InitVal initVal) {
        if (initVal.getType() == InitVal.Type.BASIC) {
            ArrayList<Integer> initVals = new ArrayList<>();
            initVals.add(this.calculator.calculateExp(initVal.exp()));
            return initVals;
        } else if (initVal.getType() == InitVal.Type.STRING) {
            return Translator.translateStringConst(initVal.stringConst());
        } else if (initVal.getType() == InitVal.Type.ARRAY) {
            ArrayList<Integer> initVals = new ArrayList<>();
            for (Exp exp : initVal.exps()) {
                initVals.add(this.calculator.calculateExp(exp));
            }
            return initVals;
        } else {
            throw new RuntimeException("When visitInitValAsConst(), got unknown type of InitVal ("
                    + initVal.getType() + ")");
        }
    }
}
