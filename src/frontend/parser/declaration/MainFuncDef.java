package frontend.parser.declaration;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.parser.statement.Block;
import frontend.type.ASTNode;
import frontend.type.TokenType;
import frontend.type.ErrorType;

import java.util.ArrayList;

public class MainFuncDef implements ASTNode {
    private final Token typeToken;
    private final Token mainToken;
    private final Token lparentToken;
    private final Token rparentToken;
    private final Block block;

    // MainFuncDef → 'int' 'main' '(' ')' Block
    public MainFuncDef(TokenStream stream) {
        String place = "MainFuncDef()";
        // 'int'
        typeToken = stream.consumeOrThrow(place, TokenType.INTTK);
        // 'main'
        mainToken = stream.consumeOrThrow(place, TokenType.MAINTK);
        // '('
        lparentToken = stream.consumeOrThrow(place, TokenType.LPARENT);
        // ')'
        rparentToken = stream.consumeOrError(place, ErrorType.MISSING_RPARENT, TokenType.RPARENT);
        // Block
        block = new Block(stream);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(typeToken);
        ret.add(mainToken);
        ret.add(lparentToken);
        ret.add(rparentToken);
        ret.add(block);
        return ret;
    }

    public Block block() {
        return block;
    }
}
