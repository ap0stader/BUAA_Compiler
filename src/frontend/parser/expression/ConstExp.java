package frontend.parser.expression;

import frontend.lexer.TokenStream;
import frontend.type.ASTNode;

import java.util.ArrayList;

public class ConstExp implements ASTNode {
    public ConstExp(TokenStream stream) {

    }

    @Override
    public ArrayList<Object> explore() {
        return null;
    }
}
