package frontend.visitor;

import frontend.error.ErrorTable;

public class Calculator {
    private final SymbolTable symbolTable;

    private final ErrorTable errorTable;

    Calculator(SymbolTable symbolTable, ErrorTable errorTable) {
        this.symbolTable = symbolTable;
        this.errorTable = errorTable;
    }
}
