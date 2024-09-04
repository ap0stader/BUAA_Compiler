package frontend.parser.expression;

import frontend.lexer.TokenStream;
import frontend.type.ASTNode;

import java.util.ArrayList;

public class Cond implements ASTNode {
    private final LOrExp lOrExp;

    // Cond → LOrExp
    public Cond(TokenStream stream) {
        lOrExp = new LOrExp(stream);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(lOrExp);
        return ret;
    }

    public LOrExp lOrExp() {
        return lOrExp;
    }
}
