package frontend.parser.declaration.variable;

import frontend.lexer.TokenStream;
import frontend.type.ASTNode;

import java.util.ArrayList;

public class InitVal implements ASTNode {
    public InitVal(TokenStream stream) {

    }

    @Override
    public ArrayList<Object> explore() {
        return null;
    }
}
