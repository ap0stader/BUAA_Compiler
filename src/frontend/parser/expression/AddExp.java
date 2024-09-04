package frontend.parser.expression;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;
import java.util.List;

public class AddExp implements ASTNode {
    private final ArrayList<MulExp> mulExps;
    private final ArrayList<Token> symbols;

    // 左递归文法：AddExp → MulExp | AddExp ('+' | '−') MulExp
    // EBNF范式： AddExp → MulExp { ('+' | '−') MulExp }
    public AddExp(TokenStream stream) {
        mulExps = new ArrayList<>();
        symbols = new ArrayList<>();
        // MulExp
        mulExps.add(new MulExp(stream));
        // { ('+' | '−') MulExp }
        while (stream.isNow(TokenType.PLUS, TokenType.MINU)) {
            symbols.add(stream.consume());
            mulExps.add(new MulExp(stream));
        }
    }

    // 按照左递归文法构建
    private AddExp(List<MulExp> mulExpsSubList, List<Token> symbolsSubList) {
        mulExps = new ArrayList<>(mulExpsSubList);
        symbols = new ArrayList<>(symbolsSubList);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        if (mulExps.size() == 1) {
            // 只有MulExp，直接返回
            ret.add(mulExps.get(0));
        } else {
            ret.add(new AddExp(mulExps.subList(0, mulExps.size() - 1), symbols.subList(0, symbols.size() - 1)));
            ret.add(symbols.get(symbols.size() - 1));
            ret.add(mulExps.get(mulExps.size() - 1));
        }
        return ret;
    }

    public ArrayList<MulExp> mulExps() {
        return mulExps;
    }

    public ArrayList<Token> symbols() {
        return symbols;
    }
}
