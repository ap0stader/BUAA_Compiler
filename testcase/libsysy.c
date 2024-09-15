#include <stdio.h>
#include "libsysy.h"
/* Input & output functions */
int getint()
{
    int t;
    scanf("%d", &t);
    while (getchar() != '\n');
    return t;
}

int getchar()
{
    char c;
    scanf("%c", &c);
    return (int)c;
}
