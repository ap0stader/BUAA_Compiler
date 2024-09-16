#include "libsysy.h"

/*
    Filename: sourcecode5.c
    Author: @Ap0stader
    Date: 2024-09-16
*/

// COMMENT AREA START
/*///****////****//*///****////****//*///****////****//*///****////****/
//*////****//*///****////****//*///****////****//*///****////****//*///*
/*///***////****////****//*///****////****//*///****////****//*///****//
///*///****////****//*///****////****//*///****////****//*///****////***
/*///****///*///****////****//*///****////****//*///****////****///****/
///*///****////****///*///****////****///*///****////**/*///****////****
// COMMENT AREA END

// CompUnit存在Decl、不存在FuncDef
int inta = 1, intb = 2, intc;
char chara = 'a', charb = 'b', charc;

int main()
{
    printf("22371345");
    printf("\n%d %d %d %d %d %d\n", inta, intb, intc, chara, charb, charc);
    int intd;
    char chard;
    // Stmt LVal = getint()
    inta = getint();
    intc = getint();
    intd = getint();
    // Stmt LVal = getchar()
    chara = getchar();
    charc = getchar();
    chard = getchar();
    printf("%d %d %d %d %d %d %d %d\n", inta, intb, intc, intd, chara, charb, charc, chard);

    int i, j, k;

    // Stmt for 无缺省
    // ForStmt
    // RelExp <=
    for(i = 0; i <= 10; i = i + 1) {
        printf("%d\n", i);
    }
    
    // Stmt for 均缺省
    k = 0;
    for(;;){
        // RelExp >=
        if (k >= 10) {
            // Stmt break
            break;
        }
        k = k + 1;
    }

    // Stmt for 缺省一个，情况1
    i = 10;
    // RelExp >
    for(; i > 0; i = i - 1) {
        printf("%d\n", i);
    }

    // Stmt for 缺省一个，情况2
    for(i = 20;; i = i - 1) {
        printf("%d\n", i);
        // ReqlExp <
        if (i < 10) {
            break;
        }
    }

    // Stmt for 缺省一个，情况3
    for(i = 30; i > 20; ) {
        printf("%d\n", i);
        i = i - 1;
    }

    // Stmt for 缺省两个，情况1
    i = 40;
    for(;; i = i - 1) {
        printf("%d\n", i);
        if (i < 30) {
            break;
        }
    }

    // Stmt for 缺省两个，情况2
    i = 50;
    for(; i > 40; ) {
        printf("%d\n", i);
        i = i - 1;
    }

    // Stmt for 缺省两个，情况4
    for(i = 60;; ) {
        printf("%d\n", i);
        if (i < 50) {
            break;
        }
        i = i - 1;
    }

    return 0;
}
