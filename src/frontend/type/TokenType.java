package frontend.type;

public enum TokenType {
    IDENFR,     // Ident
    INTCON,     // IntConst
    STRCON,     // StringConst
    CHRCON,     // CharConst

    MAINTK,     // main
    CONSTTK,    // const
    INTTK,      // int
    CHARTK,     // char
    VOIDTK,     // void
    BREAKTK,    // break
    CONTINUETK, // continue
    IFTK,       // if
    ELSETK,     // else
    FORTK,      // for
    RETURNTK,   // return

    GETINTTK,   // getint
    GETCHARTK,  // getchar
    PRINTFTK,   // printf

    PLUS,       // +
    MINU,       // -
    MULT,       // *
    DIV,        // /
    MOD,        // %

    NOT,        // !
    AND,        // &&
    OR,         // ||

    LSS,        // <
    LEQ,        // <=
    GRE,        // >
    GEQ,        // >=
    EQL,        // ==
    NEQ,        // !=

    ASSIGN,     // =
    SEMICN,     // ;
    COMMA,      // ,

    LPARENT,    // (
    RPARENT,    // )
    LBRACK,     // [
    RBRACK,     // ]
    LBRACE,     // {
    RBRACE,     // }

    EOF,        // End of file
}
