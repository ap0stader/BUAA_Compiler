package frontend.parser;

import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.parser.declaration.Decl;
import frontend.parser.declaration.FuncDef;
import frontend.parser.declaration.MainFuncDef;
import frontend.type.TokenType;

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

    // CompUnit → {Decl} {FuncDef} MainFuncDef
    public static CompUnit parse(TokenStream stream) {
        ArrayList<Decl> decls = new ArrayList<>();
        ArrayList<FuncDef> funcDefs = new ArrayList<>();
        // ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
        //                ConstDef → $Ident$ { '[' ConstExp ']' } '=' ConstInitVal
        // VarDecl → BType VarDef { $','$ VarDef } $';'$
        //        VarDef → Ident { $'['$ ConstExp ']' }
        // FuncDef → FuncType Ident $'('$ [FuncFParams] ')' Block
        while (stream.getNext(2).type() != TokenType.LPARENT) {
            decls.add(Decl.parse(stream));
        }
        // FuncDef → FuncType $Ident$ '(' [FuncFParams] ')' Block
        // MainFuncDef → 'int' $'main'$ '(' ')' Block
        while (stream.getNext(1).type() != TokenType.MAINTK) {
            funcDefs.add(FuncDef.parse(stream));
        }
        MainFuncDef mainFuncDef = MainFuncDef.parse();
        return new CompUnit(decls, funcDefs, mainFuncDef);
    }
}
