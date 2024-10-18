package frontend.visitor;

import IR.IRModule;
import IR.type.*;
import IR.value.*;
import IR.value.constant.*;
import IR.value.instruction.*;
import frontend.error.ErrorTable;
import frontend.lexer.Token;
import frontend.parser.CompUnit;
import frontend.parser.declaration.*;
import frontend.parser.declaration.constant.*;
import frontend.parser.declaration.function.*;
import frontend.parser.declaration.variable.*;
import frontend.parser.expression.*;
import frontend.parser.statement.*;

public class Vistor {
    private final CompUnit compUnit;
    private boolean finish = false;

    private final SymbolTable symbolTable;
    private final Calculator calculator;

    private final ErrorTable errorTable;

    private final IRModule irModule;

    public Vistor(CompUnit compUnit, ErrorTable errorTable) {
        this.compUnit = compUnit;
        this.errorTable = errorTable;
        this.symbolTable = new SymbolTable(errorTable);
        // 全局符号表
        symbolTable.push();
        this.calculator = new Calculator(this.symbolTable, this.errorTable);
        this.irModule = new IRModule();
    }

    public SymbolTable getSymbolTable() {
        return this.symbolTable;
    }

    public IRModule visitCompUnit() {
        if (finish) {
            return this.irModule;
        }
        // 全局变量
        for (Decl decl : this.compUnit.decls()) {

        }
        // 各函数
        for (FuncDef funcDef : this.compUnit.funcDefs()) {

        }
        // 主函数

        this.finish = true;
        return this.irModule;
    }
}
