#include "libsysy.h"

/*
    Filename: sourcecode1.c
    Author: @Ap0stader
    Date: 2024-09-16
*/

// COMMENT AREA START
///*///****////****///*///****////****///*///****////**/*///****////****
/*///****///*///****////****//*///****////****//*///****////****///****/
///*///****////****//*///****////****//*///****////****//*///****////***
//*////****//*///****////****//*///****////****//*///****////****//*///*
/*///****////****//*///****////****//*///****////****//*///****////****/
/*///***////****////****//*///****////****//*///****////****//*///****//
// COMMENT AREA END
const int constinta = 1;

void intarrayfunc(int array[], int index) {
    array[index] = 114514;
}

void chararrayfunc(char array[], int index) {
    array[index] = array[index] + 1;
}

int error(int result) {
    printf("\nShould not see this %d", result);
    return result;
}

int intfunc(char c) {
    return c + 114514;
}

char charfunc(int n) {
    return n + 1;
}

int main() {
    printf("22371345");
    int inta = 1, intb, intc;
    char chara = 'a', charb, charc;
    int intarray[constinta * 4] = {0, inta, chara};
    char chararray[constinta * 10] = "Hello";

    // LAndExp &&
    if (chararray[8] && error(1)) {
        return 0;
    }

    // LOrExp ||
    if (!intarray[0] || error(0)) {
        ;
    } else {
        return 0;
    }

    if (intarray[2] || error(0) && intarray[0] && error(1)) {
        return 0;
    }

    intb = getint();
    charb = intb;
    chararray[9] = intb;
    charc = getchar();
    intc = charc;
    intarray[3] = charc;

    printf("\n%c %c %c %d %d", charfunc(intb), charb, chararray[9], intfunc(intc), intarray[3]);

    intarrayfunc(intarray, 0);
    chararrayfunc(chararray, 1);
    printf("\n%d %d %d %d %c", intarray[0], intarray[1], intarray[2], intarray[3], chararray[1]);
    return 0;
}
