#include "libsysy.h"

/*
    Filename: sourcecode3.c
    Author: @Ap0stader
    Date: 2024-09-16
*/

// COMMENT AREA START
/*///****///*///****////****//*///****////****//*///****////****///****/
///*///****////****//*///****////****//*///****////****//*///****////***
//*////****//*///****////****//*///****////****//*///****////****//*///*
/*///****////****//*///****////****//*///****////****//*///****////****/
/*///***////****////****//*///****////****//*///****////****//*///****//
///*///****////****///*///****////****///*///****////**/*///****////****
// COMMENT AREA END

const char chara = 'a', charb = 'b';
const char chararraya[2] = {'c', 'd'};
// ConstInitVal StringConst
const char chararrayb[6] = "Hello";
char chararrayc[(-1 + 2 * 2)] = {chara, charb};
int inta = 1;

char charfunc(char character) {
    return character + 1;
}

void print(char array[], int size) {
    int i;
    printf("\n");
    for (i = 0; i < size; i = i + 1) {
        printf("%c", array[i]);
    }
    printf("\n");
}

int main() {
    printf("22371345");
    char charc = 'c';
    // InitVal StringConst
    char chararrayd[4 + 2] = "World";
    char chararraye[3] = {chara, charc};
    char chararrayf[2];
    int intb = 3;

    chararrayf[0] = getchar();
    chararrayf[inta] = getchar();
    chararrayf[intb - 1] = getchar();

    print(chararrayf, 3);
    print(chararrayd, 5);

    printf("%c %c %c", charfunc(chararraye[1]), charfunc(chararraya[0]), charfunc(chararrayb[2]));
    printf("\n%d", chararraye[2]);

    int temp;
    temp = getint();
    printf("%d", temp);
    return 0;
}
