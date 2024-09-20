#! /bin/bash

rm -f src.zip
cd src
# 清理macOS产生的垃圾文件
find . -name ".DS_Store" -type f -delete
find . -name "._*" -delete
zip -r ../src.zip ./*
echo "Finished!"