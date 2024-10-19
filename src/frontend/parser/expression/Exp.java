package frontend.parser.expression;

import frontend.lexer.TokenStream;
import frontend.type.ASTNode;

import java.util.ArrayList;

public class Exp implements ASTNode {
    private final AddExp addExp;

    // Exp → AddExp
    public Exp(TokenStream stream) {
        addExp = new AddExp(stream);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(addExp);
        return ret;
    }

    public AddExp addExp() {
        return addExp;
    }
}
