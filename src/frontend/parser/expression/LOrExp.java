package frontend.parser.expression;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;
import java.util.List;

public class LOrExp implements ASTNode {
    private final ArrayList<LAndExp> lAndExps;
    private final ArrayList<Token> symbols;

    // 左递归文法：LOrExp → LAndExp | LOrExp '||' LAndExp
    // EBNF范式： LOrExp → LAndExp { '||' LAndExp }
    public LOrExp(TokenStream stream) {
        lAndExps = new ArrayList<>();
        symbols = new ArrayList<>();
        // LAndExp
        lAndExps.add(new LAndExp(stream));
        // { '||' LAndExp }
        while (stream.isNow(TokenType.OR)) {
            symbols.add(stream.consume());
            lAndExps.add(new LAndExp(stream));
        }
    }

    private LOrExp(List<LAndExp> lAndExpsSubList, List<Token> symbolsSubList) {
        lAndExps = new ArrayList<>(lAndExpsSubList);
        symbols = new ArrayList<>(symbolsSubList);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        if (lAndExps.size() == 1) {
            // 只有LAndExp，直接返回
            ret.add(lAndExps.get(0));
        } else {
            ret.add(new LOrExp(lAndExps.subList(0, lAndExps.size() - 1), symbols.subList(0, symbols.size() - 1)));
            ret.add(symbols.get(symbols.size() - 1));
            ret.add(lAndExps.get(lAndExps.size() - 1));
        }
        return ret;
    }

    public ArrayList<LAndExp> lAndExps() {
        return lAndExps;
    }

    public ArrayList<Token> symbols() {
        return symbols;
    }
}
