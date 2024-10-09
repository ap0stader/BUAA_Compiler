package frontend.visitor;

import IR.IRModule;
import frontend.error.ErrorTable;
import frontend.parser.CompUnit;

import java.util.ArrayList;
import java.util.LinkedList;

public class Vistor {
    private final CompUnit compUnit;
    private final ErrorTable errorTable;

    private final LinkedList<SymbolTable> symbolStack;
    private final ArrayList<SymbolTable> symbolList;

    public Vistor(CompUnit compUnit, ErrorTable errorTable) {
        this.compUnit = compUnit;
        this.errorTable = errorTable;
        this.symbolStack = new LinkedList<>();
        this.symbolList = new ArrayList<>();
    }

    public ArrayList<SymbolTable> getSymbolList() {
        return this.symbolList;
    }

    public IRModule visitCompUnit() {
        return null;
    }
}
