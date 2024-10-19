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

    private final ErrorTable errorTable;

    private final IRModule irModule;

    public Visitor(CompUnit compUnit, ErrorTable errorTable) {
        this.compUnit = compUnit;
        this.errorTable = errorTable;
        this.symbolTable = new SymbolTable(errorTable);
        // 全局符号表
        symbolTable.push();
        this.calculator = new Calculator(this.symbolTable);
        this.irModule = new IRModule();
    }

    public SymbolTable getSymbolTable() {
        return this.symbolTable;
    }


    // CompUnit → {Decl} {FuncDef} MainFuncDef
    public IRModule visitCompUnit() {
        if (finish) {
            return this.irModule;
        }
        // 全局变量
        this.compUnit.decls().forEach(this::visitGlobalDecl);
        // 各函数
        this.compUnit.funcDefs().forEach(this::visitFuncDef);
        // 主函数
        this.visitMainFuncDef(this.compUnit.mainFuncDef());
        this.finish = true;
        return this.irModule;
    }

    // Decl → ConstDecl | VarDecl
    private void visitGlobalDecl(Decl decl) {
        if (decl instanceof ConstDecl constDecl) {
            for (ConstSymbol constant : this.visitConstDecl(constDecl)) {

            }
        } else if (decl instanceof VarDecl varDecl) {
            for (Pair<VarSymbol, ArrayList<Integer>> variable : this.visitGlobalVarDecl(varDecl)) {

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
            ArrayList<Integer> initVals = this.visitConstInitVal(constDef.constInitVal());
            // 字符型截取低八位
            if (bType.type() == TokenType.CHARTK) {
                initVals.replaceAll(integer -> integer & 0xff);
            }
            if (constDef.constExp() == null) {
                // 非数组
                if (initVals.size() != 1) {
                    throw new RuntimeException("When visitConstDecl(), initVals of identifier " + ident + " mismatch its type");
                }
                ConstSymbol newSymbol = new ConstSymbol(Translator.translateConstType(bType, 0), ident, initVals);
                if (this.symbolTable.insert(newSymbol)) {
                    ret.add(newSymbol);
                }
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
                ConstSymbol newSymbol = new ConstSymbol(Translator.translateConstType(bType, length), ident, initVals);
                if (this.symbolTable.insert(newSymbol)) {
                    ret.add(newSymbol);
                }
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
            if (varDef.initVal() == null) {
                throw new RuntimeException("When visitGlobalVarDecl(), initVals of identifier " + ident + " is not exist");
            }
            ArrayList<Integer> initVals = this.visitInitValAsConst(varDef.initVal());
            // 字符型截取低八位
            if (bType.type() == TokenType.CHARTK) {
                initVals.replaceAll(integer -> integer & 0xff);
            }
            if (varDef.constExp() == null) {
                // 非数组
                if (initVals.size() != 1) {
                    throw new RuntimeException("When visitGlobalVarDecl(), initVals of identifier " + ident + " mismatch its type");
                }
                VarSymbol newSymbol = new VarSymbol(Translator.translateVarType(bType, 0, false), ident);
                if (this.symbolTable.insert(newSymbol)) {
                    ret.add(new Pair<>(newSymbol, initVals));
                }
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
                VarSymbol newSymbol = new VarSymbol(Translator.translateVarType(bType, length, false), ident);
                if (this.symbolTable.insert(newSymbol)) {
                    ret.add(new Pair<>(newSymbol, initVals));
                }
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
