package frontend.visitor;

import frontend.error.ErrorTable;
import frontend.type.ErrorType;
import frontend.lexer.Token;
import frontend.type.Symbol;
import global.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class SymbolTable {
    private final ErrorTable errorTable;

    private final LinkedList<HashMap<String, Symbol<?, ?>>> subTableStack;
    private final ArrayList<ArrayList<Symbol<?, ?>>> symbolList;
    private final LinkedList<Integer> subSymbolListIndexStack;

    SymbolTable(ErrorTable errorTable) {
        this.errorTable = errorTable;
        this.subTableStack = new LinkedList<>();
        this.subSymbolListIndexStack = new LinkedList<>();
        this.symbolList = new ArrayList<>();
    }

    void push() {
        this.subTableStack.push(new HashMap<>());
        this.subSymbolListIndexStack.push(this.symbolList.size());
        this.symbolList.add(new ArrayList<>());
    }

    void pop() {
        this.subTableStack.pop();
        this.subSymbolListIndexStack.pop();
    }

    boolean insert(Symbol<?, ?> newSymbol) {
        if (this.subTableStack.isEmpty() || this.subSymbolListIndexStack.isEmpty()) {
            if (Config.visitorThrowable) {
                throw new RuntimeException("The stack of sub symbol table is empty when insert symbol '" +
                        newSymbol.name() + "' at line" + newSymbol.line());
            } else {
                return false;
            }
        } else {
            HashMap<String, Symbol<?, ?>> currentSubTable = this.subTableStack.peek();
            ArrayList<Symbol<?, ?>> currentSymbolList = this.symbolList.get(this.subSymbolListIndexStack.peek());
            if (currentSubTable.containsKey(newSymbol.name())) {
                errorTable.addErrorRecord(newSymbol.line(), ErrorType.DUPLICATED_IDENT,
                        "Duplicated symbol '" + newSymbol.name() + "' at line " + newSymbol.line() + ", " +
                                "last defined at line " + currentSubTable.get(newSymbol.name()).line());
                return false;
            } else {
                currentSubTable.put(newSymbol.name(), newSymbol);
                currentSymbolList.add(newSymbol);
                return true;
            }
        }
    }

    Symbol<?, ?> searchOrNull(Token ident) {
        if (this.subTableStack.isEmpty()) {
            if (Config.visitorThrowable) {
                throw new RuntimeException("The stack of sub symbol table is empty when search symbol '" +
                        ident.strVal() + "' at line " + ident.line());
            } else {
                return null;
            }
        } else {
            for (HashMap<String, Symbol<?, ?>> subTable : this.subTableStack) {
                if (subTable.containsKey(ident.strVal())) {
                    return subTable.get(ident.strVal());
                }
            }
            return null;
        }
    }

    Symbol<?, ?> searchOrError(Token ident) {
        Symbol<?, ?> ret = this.searchOrNull(ident);
        if (ret == null) {
            errorTable.addErrorRecord(ident.line(), ErrorType.UNDEFINED_IDENT,
                    "Undefined symbol '" + ident.strVal() + "', referenced at line " + ident.line());

        }
        return ret;
    }

    public ArrayList<ArrayList<Symbol<?, ?>>> getSymbolList() {
        return this.symbolList;
    }
}
