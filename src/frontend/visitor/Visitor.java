package frontend.visitor;

import IR.IRModule;
import frontend.error.ErrorTable;
import frontend.lexer.Token;
import frontend.parser.CompUnit;
import frontend.parser.declaration.*;
import frontend.parser.declaration.constant.ConstDecl;
import frontend.parser.declaration.constant.ConstDef;
import frontend.parser.declaration.constant.ConstInitVal;
import frontend.parser.declaration.function.*;
import frontend.parser.declaration.variable.*;
import frontend.parser.expression.ConstExp;
import frontend.type.TokenType;
import frontend.visitor.symbol.ConstSymbol;
import frontend.visitor.symbol.VarSymbol;

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
            for (ConstSymbol constSymbol : this.visitConstDecl(constDecl)) {

            }
        } else if (decl instanceof VarDecl varDecl) {
            for (VarSymbol varSymbol : this.visitGlobalVarDecl(varDecl)) {

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
        Token BType = constDecl.typeToken();
        // visitConstDef()
        for (ConstDef constDef : constDecl.constDefs()) {
            Token ident = constDef.ident();
            ArrayList<Integer> initVals = this.visitConstInitVal(constDef.constInitVal());
            // 字符型截取低八位
            if (BType.type() == TokenType.CHARTK) {
                initVals.replaceAll(integer -> integer & 0xff);
            }
            if (constDef.constExp() == null) {
                // 非数组
                if (initVals.size() != 1) {
                    throw new RuntimeException("When visitConstDecl(), initVals of identify " + ident + " mismatch its type");
                }
                ConstSymbol newSymbol = new ConstSymbol(Translator.translateConstType(BType, 0), ident, initVals);
                if (this.symbolTable.insert(newSymbol)) {
                    ret.add(newSymbol);
                }
            } else {
                // 数组
                int length = this.calculator.calculateConstExp(constDef.constExp());
                if (initVals.size() > length) {
                    throw new RuntimeException("When visitConstDecl(), initVals of identify " + ident + " is longer than its length");
                } else {
                    // 补齐未显示写出的0
                    for (int i = initVals.size(); i < length; i++) {
                        initVals.add(0);
                    }
                }
                ConstSymbol newSymbol = new ConstSymbol(Translator.translateConstType(BType, length), ident, initVals);
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
    // InitVal → Exp | '{' [ Exp { ',' Exp } ] '}' | StringConst
    private ArrayList<VarSymbol> visitGlobalVarDecl(VarDecl varDecl) {
        ArrayList<VarSymbol> ret = new ArrayList<>();

        return ret;
    }
}
