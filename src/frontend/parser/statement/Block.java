package frontend.parser.statement;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;

public class Block implements ASTNode {
    private final Token lbraceToken;
    private final ArrayList<BlockItem> blockItems;
    private final Token rbraceToken;

    // Block → '{' { BlockItem } '}'
    public Block(TokenStream stream) {
        String place = "Block()";
        // '{'
        lbraceToken = stream.consumeOrThrow(place, TokenType.LBRACE);
        // { BlockItem }
        blockItems = new ArrayList<>();
        while (stream.getNow().type() != TokenType.RBRACE) {
            blockItems.add(BlockItem.parse(stream));
        }
        // '}'
        rbraceToken = stream.consumeOrThrow(place, TokenType.RBRACE);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(lbraceToken);
        ret.addAll(blockItems);
        ret.add(rbraceToken);
        return ret;
    }

    public ArrayList<BlockItem> blockItems() {
        return blockItems;
    }
}
