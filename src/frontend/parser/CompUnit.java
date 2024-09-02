package frontend.parser;

import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.parser.ast.Decl;
import frontend.parser.ast.FuncDef;
import frontend.parser.ast.MainFuncDef;

import java.util.ArrayList;

public record CompUnit(
        ArrayList<Decl> decls,
        ArrayList<FuncDef> funcDefs,
        MainFuncDef mainFuncDef
) implements ASTNode {
    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.addAll(decls);
        ret.addAll(funcDefs);
        ret.add(mainFuncDef);
        return ret;
    }

    public static CompUnit parse(TokenStream stream) {
        return new CompUnit(null, null, null);
    }
}
