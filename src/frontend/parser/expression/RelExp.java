package frontend.parser.expression;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;
import java.util.List;

public class RelExp implements ASTNode {
    private final ArrayList<AddExp> addExps;
    private final ArrayList<Token> symbols;

    // 左递归文法：RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    // EBNF范式： RelExp → AddExp { ('<' | '>' | '<=' | '>=') AddExp }
    public RelExp(TokenStream stream) {
        addExps = new ArrayList<>();
        symbols = new ArrayList<>();
        // AddExp
        addExps.add(new AddExp(stream));
        // { ('<' | '>' | '<=' | '>=') AddExp }
        while (stream.isNow(TokenType.LSS, TokenType.GRE, TokenType.LEQ, TokenType.GEQ)) {
            symbols.add(stream.consume());
            addExps.add(new AddExp(stream));
        }
    }

    private RelExp(List<AddExp> RelExpsSubList, List<Token> symbolsSubList) {
        addExps = new ArrayList<>(RelExpsSubList);
        symbols = new ArrayList<>(symbolsSubList);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        if (addExps.size() == 1) {
            // 只有AddExp，正常返回
            ret.add(addExps.get(0));
        } else {
            ret.add(new RelExp(addExps.subList(0, addExps.size() - 1), symbols.subList(0, symbols.size() - 1)));
            ret.add(symbols.get(symbols.size() - 1));
            ret.add(addExps.get(addExps.size() - 1));
        }
        return ret;
    }

    public ArrayList<AddExp> addExps() {
        return addExps;
    }

    public ArrayList<Token> symbols() {
        return symbols;
    }
}
