package frontend.parser.expression;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;
import java.util.List;

public class MulExp implements ASTNode {
    private final ArrayList<UnaryExp> unaryExps;
    private final ArrayList<Token> symbols;

    // 左递归文法：MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    // EBNF范式： MulExp → UnaryExp { ('*' | '/' | '%') UnaryExp }
    MulExp(TokenStream stream) {
        unaryExps = new ArrayList<>();
        symbols = new ArrayList<>();
        // UnaryExp
        unaryExps.add(UnaryExp.parse(stream));
        // { ('*' | '/' | '%') UnaryExp }
        while (stream.isNow(TokenType.MULT, TokenType.DIV, TokenType.MOD)) {
            symbols.add(stream.consume());
            unaryExps.add(UnaryExp.parse(stream));
        }
    }

    // 按照左递归文法构建
    private MulExp(List<UnaryExp> unaryExpsSubList, List<Token> symbolsSubList) {
        unaryExps = new ArrayList<>(unaryExpsSubList);
        symbols = new ArrayList<>(symbolsSubList);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        if (unaryExps.size() == 1) {
            // 只有UnaryExp，直接返回
            ret.add(unaryExps.get(0));
        } else {
            ret.add(new MulExp(unaryExps.subList(0, unaryExps.size() - 1), symbols.subList(0, symbols.size() - 1)));
            ret.add(symbols.get(symbols.size() - 1));
            ret.add(unaryExps.get(unaryExps.size() - 1));
        }
        return ret;
    }

    public ArrayList<UnaryExp> unaryExps() {
        return unaryExps;
    }

    public ArrayList<Token> symbols() {
        return symbols;
    }
}
