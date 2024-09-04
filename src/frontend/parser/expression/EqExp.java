package frontend.parser.expression;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;
import java.util.List;

public class EqExp implements ASTNode {
    private final ArrayList<RelExp> relExps;
    private final ArrayList<Token> symbols;

    // 左递归文法：EqExp → RelExp | EqExp ('==' | '!=') RelExp
    // EBNF范式： EqExp → RelExp { ('==' | '!=') RelExp }
    EqExp(TokenStream stream) {
        relExps = new ArrayList<>();
        symbols = new ArrayList<>();
        // RelExp
        relExps.add(new RelExp(stream));
        // { ('==' | '!=') RelExp }
        while (stream.isNow(TokenType.EQL, TokenType.NEQ)) {
            symbols.add(stream.consume());
            relExps.add(new RelExp(stream));
        }
    }

    private EqExp(List<RelExp> EqExpsSubList, List<Token> symbolsSubList) {
        relExps = new ArrayList<>(EqExpsSubList);
        symbols = new ArrayList<>(symbolsSubList);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        if (relExps.size() == 1) {
            // 只有RelExp，正常返回
            ret.add(relExps.get(0));
        } else {
            ret.add(new EqExp(relExps.subList(0, relExps.size() - 1), symbols.subList(0, symbols.size() - 1)));
            ret.add(symbols.get(symbols.size() - 1));
            ret.add(relExps.get(relExps.size() - 1));
        }
        return ret;
    }

    public ArrayList<RelExp> relExps() {
        return relExps;
    }

    public ArrayList<Token> symbols() {
        return symbols;
    }
}
