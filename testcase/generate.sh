#! /bin/bash
rm -rf src
mkdir src
rm -f src.zip

for (( i=1; i<=6; i=i+1 )); do
    gcc "sourcecode${i}.c" "libsysy.c" -o "program${i}.out" -Wno-comment
    cp "sourcecode${i}.c" "./src/testfile${i}.txt"
    sed -i '' '/#include "libsysy.h"/d' "./src/testfile${i}.txt"
    cp "input${i}.txt" "./src/input${i}.txt"
    ./program${i}.out < "input${i}.txt" > "./src/output${i}.txt"
    rm "program${i}.out"
done

zip -r src.zip ./src/*