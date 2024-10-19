package frontend.parser.expression;

import frontend.lexer.TokenStream;
import frontend.type.ASTNode;

import java.util.ArrayList;

public class ConstExp implements ASTNode {
    private final AddExp addExp;

    // Exp → AddExp
    // 注：使用的Ident必须是常量，具体判断不在语法分析中完成
    public ConstExp(TokenStream stream) {
        addExp = new AddExp(stream);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(addExp);
        return ret;
    }

    public AddExp addExp() {
        return addExp;
    }
}
