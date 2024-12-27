#! /bin/bash

find * | grep -v 'MARS.jar\|testfile.c\|libsysy.*\|.sh' | xargs rm -rf

sed '/#include/d' "testfile.c" > testfile.txt
