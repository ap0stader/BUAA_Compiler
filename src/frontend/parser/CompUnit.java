package frontend.parser;

import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.parser.declaration.Decl;
import frontend.parser.declaration.function.FuncDef;
import frontend.parser.declaration.MainFuncDef;
import frontend.type.TokenType;

import java.util.ArrayList;

public class CompUnit implements ASTNode {
    private final ArrayList<Decl> decls;
    private final ArrayList<FuncDef> funcDefs;
    private final MainFuncDef mainFuncDef;

    // CompUnit → {Decl} {FuncDef} MainFuncDef
    public CompUnit(TokenStream stream) {
        decls = new ArrayList<>();
        funcDefs = new ArrayList<>();
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
            funcDefs.add(new FuncDef(stream));
        }
        mainFuncDef = new MainFuncDef(stream);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.addAll(decls);
        ret.addAll(funcDefs);
        ret.add(mainFuncDef);
        return ret;
    }

    public ArrayList<Decl> decls() {
        return decls;
    }

    public ArrayList<FuncDef> funcDefs() {
        return funcDefs;
    }

    public MainFuncDef mainFuncDef() {
        return mainFuncDef;
    }
}
