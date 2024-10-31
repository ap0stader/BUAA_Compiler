package frontend.parser.expression;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;
import java.util.List;

public class LAndExp implements ASTNode {
    private final ArrayList<EqExp> eqExps;
    private final ArrayList<Token> symbols;

    // 左递归文法：LAndExp → EqExp | LAndExp '&&' EqExp
    // EBNF范式： LAndExp → EqExp { '&&' EqExp }
    LAndExp(TokenStream stream) {
        eqExps = new ArrayList<>();
        symbols = new ArrayList<>();
        // LAndExp
        eqExps.add(new EqExp(stream));
        // { '&&' LAndExp }
        while (stream.isNow(TokenType.AND)) {
            symbols.add(stream.consume());
            eqExps.add(new EqExp(stream));
        }
    }

    private LAndExp(List<EqExp> lAndExpsSubList, List<Token> symbolsSubList) {
        eqExps = new ArrayList<>(lAndExpsSubList);
        symbols = new ArrayList<>(symbolsSubList);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        if (eqExps.size() == 1) {
            // 只有EqExp，正常返回
            ret.add(eqExps.get(0));
        } else {
            ret.add(new LAndExp(eqExps.subList(0, eqExps.size() - 1), symbols.subList(0, symbols.size() - 1)));
            ret.add(symbols.get(symbols.size() - 1));
            ret.add(eqExps.get(eqExps.size() - 1));
        }
        return ret;
    }

    public ArrayList<EqExp> eqExps() {
        return eqExps;
    }
}
