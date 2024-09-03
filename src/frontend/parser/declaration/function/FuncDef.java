package frontend.parser.declaration.function;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.parser.statement.Block;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;

public class FuncDef implements ASTNode {
    private final FuncType funcType;
    private final Token ident;
    private final Token lparent;
    private final FuncFParams funcFParams;
    private final Token rparent;
    private final Block block;

    // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    public FuncDef(TokenStream stream) {
        String place = "FuncDef()";
        // FuncType
        funcType = new FuncType(stream);
        // Ident
        ident = stream.consumeOrThrow(place, TokenType.IDENFR);
        // '('
        lparent = stream.consumeOrThrow(place, TokenType.LPARENT);
        // [FuncFParams]
        // FuncFParams → FuncFParam { ',' FuncFParam }
        //  FuncFParam → 'int' Ident ['[' ']' { '[' ConstExp ']' }]
        funcFParams = stream.isNow(TokenType.INTTK) ? new FuncFParams(stream) : null;
        // ')'
        rparent = stream.consumeOrThrow(place, TokenType.RPARENT);
        // Block
        block = new Block(stream);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(funcType);
        ret.add(ident);
        ret.add(lparent);
        if (funcFParams != null) {
            ret.add(funcFParams);
        }
        ret.add(rparent);
        ret.add(block);
        return ret;
    }

    public FuncType funcType() {
        return funcType;
    }

    public Token ident() {
        return ident;
    }

    public FuncFParams funcFParams() {
        return funcFParams;
    }

    public Block block() {
        return block;
    }
}
