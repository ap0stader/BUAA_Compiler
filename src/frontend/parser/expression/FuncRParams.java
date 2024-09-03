package frontend.parser.expression;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;

public class FuncRParams implements ASTNode {
    // FuncRParams → Exp { ',' Exp }
    private final ArrayList<Exp> exps;
    private final ArrayList<Token> commaTokens;

    public FuncRParams(TokenStream stream) {
        // Exp
        exps = new ArrayList<>();
        exps.add(new Exp(stream));
        // { ',' Exp }
        commaTokens = new ArrayList<>();
        while (stream.isNow(TokenType.COMMA)) {
            commaTokens.add(stream.consume());
            exps.add(new Exp(stream));
        }
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(exps.get(0));
        for (int i = 1; i < exps.size(); i++) {
            ret.add(commaTokens.get(i - 1));
            ret.add(exps.get(i));
        }
        return ret;
    }

    public ArrayList<Exp> exps() {
        return exps;
    }
}
