package frontend.type;

public enum TokenType {
    IDENFR,     // Ident
    INTCON,     // IntConst
    STRCON,     // FormatString

    MAINTK,     // main
    CONSTTK,    // const
    INTTK,      // int
    VOIDTK,     // void
    BREAKTK,    // break
    CONTINUETK, // continue
    IFTK,       // if
    ELSETK,     // else
    FORTK,      // for
    RETURNTK,   // return

    GETINTTK,   // getint
    PRINTFTK,   // printf

    NOT,        // !
    AND,        // &&
    OR,         // ||

    PLUS,       // +
    MINU,       // -
    MULT,       // *
    DIV,        // /
    MOD,        // %

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
