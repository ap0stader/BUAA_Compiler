#include "libsysy.h"

/*
    Filename: sourcecode4.c
    Author: @Ap0stader
    Date: 2024-09-16
*/

// COMMENT AREA START
//*////****//*///****////****//*///****////****//*///****////****//*///*
/*///****////****//*///****////****//*///****////****//*///****////****/
/*///***////****////****//*///****////****//*///****////****//*///****//
///*///****////****//*///****////****//*///****////****//*///****////***
/*///****///*///****////****//*///****////****//*///****////****///****/
///*///****////****///*///****////****///*///****////**/*///****////****
// COMMENT AREA END

// CompUnit不存在Decl、存在FuncDef

// FuncDef 有形参
// FuncType 'int'
// FuncFParams 花括号内重复多次
// FuncFParam 普通变量
int intfunc(int a, char b)
{
    // Stmt return 有Exp
    return a + b;
}

// FuncType 'char'
// FuncFParams 花括号内重复0次
char charfunc(char c)
{
    return c - '1' + 1;
}

// FuncDef 无形参
// FuncType 'void'
void print() {
    // Stmt printf 无Exp
    printf("\nCall void with return");
    // Stmt return 无Exp
    return;
}

void print2() {
    printf("Call void without return\n");
}

// MainFuncDef
int main()
// Block 花括号内重复多次
{   
    printf("22371345");
    // Block 花括号内重复0次
    // BlockItem Stmt
    // Stmt Block
    {{{{{{{{{{{{{}}}}}}}}}}}}}
    // Stmt ;
    ;;;;;;;;;;;;;;;;;;;;;;;;;;
    // Decl ConstDecl
    // ConstDecl 花括号内重复0次
    // BType 'int'
    // ConstDef 普通变量
    // ConstInitVal 常表达式初值
    // Block Decl
    // ConstExp
    // PrimaryExp Number
    // Number
    // UnaryExp PrimaryExp
    // MulExp UnaryExp
    // AddExp MulExp
    const int coninta = 1;
    // ConstDecl 花括号重复多次
    // BType 'char'
    // PrimaryExp Character
    // Character
    const char conchara = 'a', concharb = 'b', concharc = 99;
    // Decl VarDecl
    // VarDecl 花括号内重复多次
    // VarDef 普通变量 有初值
    // VarDef 普通变量 无初值
    // InitVal 表达式初值
    // Exp
    int inta = 2, intb;
    // VarDecl 花括号内重复0次
    int intc;
    // PrimaryExp (Exp)
    // Lval 普通变量
    // PrimaryExp LVal
    char chara = ('d' - inta), charb;
    char charc;

    // Stmt if 无else
    // Cond
    // UnaryExp UnaryOP
    // UnaryOP !
    // LOrExp LAndExp
    // LAndExp EqExp
    // EqExp RelExp
    // RelExp AddExp
    if (!(inta)) {
        return 0;
    }

    // Stmt LVal = Exp
    // LVal 普通常量
    // UnaryExp Ident() 有参数
    // FunRParams 花括号内重复多次
    // FunRParams 花括号内重复0次
    // AddExp +
    intb = concharb + intfunc(inta, coninta) + charfunc(conchara);
    // UnaryOP +
    // UnaryOP -
    // AddExp -
    intc = -3 - (+3);

    // MulExp * / %
    // AddExp
    int tempd = 1 * 2 / 3 % 4;

    // EqExp !=
    if (tempd != -1){
        return 0;
    }

    // Stmt Exp;
    // UnaryExp Ident() 无参数
    print();
    print2();

    // Stmt if 有else
    // EqExp ==
    if (intc == -5) {
        return 0;
    } else {
        // Stmt printf 有Exp
        printf("%d %d %d %d %d %d %d %d %d\n%c %c %c %c ", coninta, inta, intb, intc, tempd, conchara, concharb, concharc, chara, conchara, concharb, concharc, chara + 1);
    }
    return 0;
}
