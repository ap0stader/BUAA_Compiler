#! /bin/bash

if [ ! -e "libsysy.ll" ]; then
    clang -S -emit-llvm libsysy.c -o libsysy.ll
fi

llvm-link llvm_ir.txt libsysy.ll -S -o out.ll
lli out.ll
