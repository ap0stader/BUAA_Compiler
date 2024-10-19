package output;

import frontend.visitor.symbol.ConstSymbol;
import frontend.visitor.symbol.Symbol;
import frontend.visitor.SymbolTable;
import global.Config;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class DumpSymbolTable {
    public static void dump(SymbolTable symbolTable) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(Config.dumpSymbolTableFileName));
        ArrayList<ArrayList<Symbol>> subSymbolLists = symbolTable.getSymbolList();
        for (int level = 1; level <= subSymbolLists.size(); level++) {
            for (Symbol symbol : subSymbolLists.get(level - 1)) {
                if (Config.dumpSymbolTableDetail) {
                    if (symbol instanceof ConstSymbol constSymbol) {
                        out.write(level + " " + symbol.line() + " "
                                + symbol.name() + " " + symbol.typeDisplayStr() + " " + constSymbol.initVals() + "\n");
                    } else {
                        out.write(level + " " + symbol.line() + " "
                                + symbol.name() + " " + symbol.typeDisplayStr() + "\n");
                    }
                } else {
                    out.write(level + " " + symbol.name() + " " + symbol.typeDisplayStr() + "\n");
                }
            }
        }
        out.close();
    }

    private DumpSymbolTable() {
    }
}
