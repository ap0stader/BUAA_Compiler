package frontend.parser.declaration.variable;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.parser.declaration.Decl;
import frontend.type.ASTNode;
import frontend.type.TokenType;
import global.error.ErrorType;

import java.util.ArrayList;

public class VarDecl implements ASTNode, Decl {
    private final Token typeToken;
    private final ArrayList<VarDef> varDefs;
    private final ArrayList<Token> commaTokens;
    private final Token semicnToken;

    // VarDecl → 'int' VarDef { ',' VarDef } ';'
    public VarDecl(TokenStream stream) {
        String place = "VarDecl()";
        // 'int'
        typeToken = stream.consumeOrThrow(place, TokenType.INTTK);
        // VarDef
        varDefs = new ArrayList<>();
        varDefs.add(new VarDef(stream));
        // { ',' VarDef }
        commaTokens = new ArrayList<>();
        while (stream.isNow(TokenType.COMMA)) {
            commaTokens.add(stream.consume());
            varDefs.add(new VarDef(stream));
        }
        // ';'
        semicnToken = stream.consumeOrError(place, ErrorType.MISSING_SEMICN, TokenType.SEMICN);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(typeToken);
        ret.add(varDefs.get(0));
        for (int i = 1; i < varDefs.size(); i++) {
            ret.add(commaTokens.get(i - 1));
            ret.add(varDefs.get(i));
        }
        ret.add(semicnToken);
        return ret;
    }

    public ArrayList<VarDef> varDefs() {
        return varDefs;
    }
}
