int gua10000[10000];
int gua0[0];

int ia[10] = {1, 2};
int lia[1000] = {1, 2};
const int cia[10] = {1, 2};
const int lcia[1000] = {1, 2};

char ca[10] = {'a', 'b'};
char lca[1000] = {'a', 'b'};
char cas[10] = "ab";
char lcas[1000] = "ab";
const char cca[10] = {'a', 'b'};
const char lcca[1000] = {'a', 'b'};
const char ccas[10] = "ab";
const char lccas[1000] = "ab";

void int_func(int a) {}

void char_func(char a) {}

void local_array() {
    int lua10000[10000];
    int lua0[0];

    int ia_local[10] = {1, 2};
    int lia_local[1000] = {1, 2};
    const int cia_local[10] = {1, 2};
    const int lcia_local[1000] = {1, 2};

    char ca_local[10] = {'a', 'b'};
    char lca_local[1000] = {'a', 'b'};
    char cas_local[10] = "ab";
    char lcas_local[1000] = "ab";
    const char cca_local[10] = {'a', 'b'};
    const char lcca_local[1000] = {'a', 'b'};
    const char ccas_local[10] = "ab";
    const char lccas_local[1000] = "ab";

    int_func(ia_local[0]);
    int_func(lia_local[0]);
    ia_local[2] = 3;
    lia_local[2] = 3;

    int_func(cia_local[0]);
    int_func(lcia_local[0]);

    char_func(ca_local[0]);
    char_func(lca_local[0]);
    char_func(cas_local[0]);
    char_func(lcas_local[0]);
    ca_local[2] = 'c';
    lca_local[2] = 'c';
    cas_local[2] = 'c';
    lcas_local[2] = 'c';

    char_func(cca_local[0]);
    char_func(lcca_local[0]);
    char_func(ccas_local[0]);
    char_func(lccas_local[0]);
}

int main() {
    int_func(ia[0]);
    int_func(lia[0]);
    ia[2] = 3;
    lia[2] = 3;

    int_func(cia[0]);
    int_func(lcia[0]);

    char_func(ca[0]);
    char_func(lca[0]);
    char_func(cas[0]);
    char_func(lcas[0]);
    ca[2] = 'c';
    lca[2] = 'c';
    cas[2] = 'c';
    lcas[2] = 'c';

    char_func(cca[0]);
    char_func(lcca[0]);
    char_func(ccas[0]);
    char_func(lccas[0]);
    
    return 0;
}
