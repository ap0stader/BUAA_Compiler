package frontend.parser.declaration.constant;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.parser.declaration.Decl;
import frontend.type.ASTNode;
import frontend.type.TokenType;
import global.error.ErrorType;

import java.util.ArrayList;

public class ConstDecl implements ASTNode, Decl {
    private final Token constToken;
    private final Token typeToken;
    private final ArrayList<ConstDef> constDefs;
    private final ArrayList<Token> commaTokens;
    private final Token semicnToken;

    // ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
    public ConstDecl(TokenStream stream) {
        String place = "ConstDecl()";
        // 'const'
        constToken = stream.consumeOrThrow(place, TokenType.CONSTTK);
        // BType → 'int' | 'char'
        typeToken = stream.consumeOrThrow(place, TokenType.INTTK, TokenType.CHARTK);
        // ConstDef
        constDefs = new ArrayList<>();
        constDefs.add(new ConstDef(stream));
        // { ',' ConstDef }
        commaTokens = new ArrayList<>();
        while (stream.isNow(TokenType.COMMA)) {
            commaTokens.add(stream.consume());
            constDefs.add(new ConstDef(stream));
        }
        // ';'
        semicnToken = stream.consumeOrError(place, ErrorType.MISSING_SEMICN, TokenType.SEMICN);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(constToken);
        ret.add(typeToken);
        ret.add(constDefs.get(0));
        for (int i = 1; i < constDefs.size(); i++) {
            ret.add(commaTokens.get(i - 1));
            ret.add(constDefs.get(i));
        }
        ret.add(semicnToken);
        return ret;
    }

    public Token typeToken() {
        return typeToken;
    }

    public ArrayList<ConstDef> constDefs() {
        return constDefs;
    }
}
