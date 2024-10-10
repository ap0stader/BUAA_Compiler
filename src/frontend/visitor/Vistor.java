package frontend.visitor;

import IR.IRModule;
import frontend.error.ErrorTable;
import frontend.parser.CompUnit;

public class Vistor {
    private final CompUnit compUnit;
    private final ErrorTable errorTable;

    private final SymbolTable symbolTable;

    public Vistor(CompUnit compUnit, ErrorTable errorTable) {
        this.compUnit = compUnit;
        this.errorTable = errorTable;
        this.symbolTable = new SymbolTable(errorTable);
    }

    public SymbolTable getSymbolTable() {
        return this.symbolTable;
    }

    public IRModule visitCompUnit() {
        symbolTable.push();

        return null;
    }
}
