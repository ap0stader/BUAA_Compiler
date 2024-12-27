#include "libsysy.h"

/*
    Filename: sourcecode2.c
    Author: @Ap0stader
    Date: 2024-09-16
*/

// COMMENT AREA START
/*///****///*///****////****//*///****////****//*///****////****///****/
//*////****//*///****////****//*///****////****//*///****////****//*///*
/*///****////****//*///****////****//*///****////****//*///****////****/
/*///***////****////****//*///****////****//*///****////****//*///****//
///*///****////****//*///****////****//*///****////****//*///****////***
///*///****////****///*///****////****///*///****////**/*///****////****
// COMMENT AREA END

const int inta = 1, intb = 2;
// ConstDef 一维数组
// ConstInitVal 一维数组初值
const int intarraya[3] = {1, 2, 3};
// VarDef 一维数组
// InitVal 一维数组初值
int intarrayb[(-inta + intb * 2)] = {inta, inta + intb};

// FuncFParam 一维数组
int intarrayfunc(int array[], int size) {
    int i, result = 0;
    for (i = 0; i < size; i = i + 1) {
        result = result + array[i];
    }
    return result;
}

int intfunc(int number) {
    return number * number;
}

int main() {
    printf("22371345");
    int intc = 3;
    int intarrayc[inta + intb] = {inta, intc};
    int intarrayd[4];

    // LVal 一维数组
    intarrayd[0] = getint();
    intarrayd[inta] = getint();
    intarrayd[intc - 1] = getint();

    // FuncRParams 一维数组整体传参
    printf("\n%d %d", intarrayfunc(intarrayc, 3), intarrayfunc(intarrayd, 2));
    // FuncRParams 一维数组部分传参
    printf("\n%d %d", intfunc(intarraya[2]), intarrayb[2]);

    return 0;
}
