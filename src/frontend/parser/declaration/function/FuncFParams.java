package frontend.parser.declaration.function;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;

public class FuncFParams implements ASTNode {
    private final ArrayList<FuncFParam> funcFParams;
    private final ArrayList<Token> commaTokens;

    // FuncFParams → FuncFParam { ',' FuncFParam }
    FuncFParams(TokenStream stream) {
        // FuncFParam
        funcFParams = new ArrayList<>();
        funcFParams.add(new FuncFParam(stream));
        // { ',' FuncFParam }
        commaTokens = new ArrayList<>();
        while (stream.isNow(TokenType.COMMA)) {
            commaTokens.add(stream.consume());
            funcFParams.add(new FuncFParam(stream));
        }
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(funcFParams.get(0));
        for (int i = 1; i < funcFParams.size(); i++) {
            ret.add(commaTokens.get(i - 1));
            ret.add(funcFParams.get(i));
        }
        return ret;
    }

    public ArrayList<FuncFParam> funcFParams() {
        return funcFParams;
    }
}
