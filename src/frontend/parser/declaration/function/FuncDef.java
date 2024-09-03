package frontend.parser.declaration.function;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.parser.statement.Block;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;

public class FuncDef implements ASTNode {
    public enum Type {
        NO_FPRAMS,
        WITH_FPRAMS,
    }

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
        if (stream.isNow(TokenType.INTTK)) {
            funcFParams = new FuncFParams(stream);
        } else {
            funcFParams = null;
        }
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
        if (this.getType() == Type.WITH_FPRAMS) {
            ret.add(funcFParams);
        }
        ret.add(rparent);
        ret.add(block);
        return ret;
    }

    public Type getType() {
        return funcFParams == null ? Type.NO_FPRAMS : Type.WITH_FPRAMS;
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
