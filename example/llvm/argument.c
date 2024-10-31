int a = 2;

int arrayfunca(int a[]) {
    a[1] = 1;
    return a[1];
}

int arrayfuncb(int b[]) {
    return arrayfunca(b);
}

int main() {
    int a = 3;
    int array[2] = {1, a};
    return arrayfunca(array);
}
