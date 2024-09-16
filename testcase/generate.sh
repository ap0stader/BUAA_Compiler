#! /bin/bash

# 清理目录环境
rm -rf src
mkdir src
rm -f src.zip

for (( i=1; i<=6; i=i+1 )); do
    # 忽略有意的注释导致的警告
    gcc "sourcecode${i}.c" "libsysy.c" -o "program${i}.out" -Wno-comment
    cp "sourcecode${i}.c" "./src/testfile${i}.txt"
    # 删除不应该出现的#include
    sed -i '' '/#include "libsysy.h"/d' "./src/testfile${i}.txt"
    cp "input${i}.txt" "./src/input${i}.txt"
    # 补充换行符到文件结尾避免出现死循环
    echo "" >> "./src/input${i}.txt"
    ./program${i}.out < "./src/input${i}.txt" > "./src/output${i}.txt"
    rm "program${i}.out"
    echo "Sourcecode ${i} done!"
done

cd src
zip ../src.zip ./*
echo "Finished!"
