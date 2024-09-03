package frontend.parser.expression;

import frontend.lexer.TokenStream;
import frontend.type.ASTNode;

public class ConstExp extends Exp implements ASTNode {
    // ConstExp → AddExp
    // 注：使用的Ident必须是常量，具体判断不在语法分析中完成
    public ConstExp(TokenStream stream) {
        super(stream);
    }
}
