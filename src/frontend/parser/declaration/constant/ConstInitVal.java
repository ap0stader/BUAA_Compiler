package frontend.parser.declaration.constant;

import frontend.lexer.TokenStream;
import frontend.type.ASTNode;

import java.util.ArrayList;

public class ConstInitVal implements ASTNode {
    public ConstInitVal(TokenStream stream) {

    }

    @Override
    public ArrayList<Object> explore() {
        return null;
    }
}
