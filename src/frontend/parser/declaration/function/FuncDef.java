package frontend.parser.declaration.function;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.parser.statement.Block;
import frontend.type.ASTNode;
import frontend.type.TokenType;
import global.error.ErrorType;

import java.util.ArrayList;

public class FuncDef implements ASTNode {
    private final FuncType funcType;
    private final Token ident;
    private final Token lparentToken;
    private final FuncFParams funcFParams;
    private final Token rparentToken;
    private final Block block;

    // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    public FuncDef(TokenStream stream) {
        String place = "FuncDef()";
        // FuncType
        funcType = new FuncType(stream);
        // Ident
        ident = stream.consumeOrThrow(place, TokenType.IDENFR);
        // '('
        lparentToken = stream.consumeOrThrow(place, TokenType.LPARENT);
        // [FuncFParams]
        // FuncFParams → FuncFParam { ',' FuncFParam }
        //  FuncFParam → BType Ident ['[' ']']
        //       BType → 'int' | 'char'
        funcFParams = stream.isNow(TokenType.INTTK, TokenType.CHARTK) ? new FuncFParams(stream) : null;
        // ')'
        rparentToken = stream.consumeOrError(place, ErrorType.MISSING_RPARENT, TokenType.RPARENT);
        // Block
        block = new Block(stream);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(funcType);
        ret.add(ident);
        ret.add(lparentToken);
        if (funcFParams != null) {
            ret.add(funcFParams);
        }
        ret.add(rparentToken);
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
