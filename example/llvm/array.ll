; ModuleID = 'array.c'
source_filename = "array.c"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

@ia = dso_local global <{ i32, i32, [8 x i32] }> <{ i32 1, i32 2, [8 x i32] zeroinitializer }>, align 16
@lia = dso_local global <{ i32, i32, [998 x i32] }> <{ i32 1, i32 2, [998 x i32] zeroinitializer }>, align 16
@cia = dso_local constant <{ i32, i32, [8 x i32] }> <{ i32 1, i32 2, [8 x i32] zeroinitializer }>, align 16
@lcia = dso_local constant <{ i32, i32, [998 x i32] }> <{ i32 1, i32 2, [998 x i32] zeroinitializer }>, align 16
@ca = dso_local global <{ i8, i8, [8 x i8] }> <{ i8 97, i8 98, [8 x i8] zeroinitializer }>, align 1
@lca = dso_local global <{ i8, i8, [998 x i8] }> <{ i8 97, i8 98, [998 x i8] zeroinitializer }>, align 16
@cas = dso_local global [10 x i8] c"ab\00\00\00\00\00\00\00\00", align 1
@lcas = dso_local global [1000 x i8] c"ab\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00", align 16
@cca = dso_local constant <{ i8, i8, [8 x i8] }> <{ i8 97, i8 98, [8 x i8] zeroinitializer }>, align 1
@lcca = dso_local constant <{ i8, i8, [998 x i8] }> <{ i8 97, i8 98, [998 x i8] zeroinitializer }>, align 16
@ccas = dso_local constant [10 x i8] c"ab\00\00\00\00\00\00\00\00", align 1
@lccas = dso_local constant [1000 x i8] c"ab\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00", align 16
@__const.local_array.ca_local = private unnamed_addr constant <{ i8, i8, [8 x i8] }> <{ i8 97, i8 98, [8 x i8] zeroinitializer }>, align 1
@__const.local_array.cas_local = private unnamed_addr constant [10 x i8] c"ab\00\00\00\00\00\00\00\00", align 1
@__const.local_array.cca_local = private unnamed_addr constant <{ i8, i8, [8 x i8] }> <{ i8 97, i8 98, [8 x i8] zeroinitializer }>, align 1
@__const.local_array.ccas_local = private unnamed_addr constant [10 x i8] c"ab\00\00\00\00\00\00\00\00", align 1
@gua10000 = common dso_local global [10000 x i32] zeroinitializer, align 16
@gua0 = common dso_local global [0 x i32] zeroinitializer, align 4

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @int_func(i32 %0) #0 {
  %2 = alloca i32, align 4
  store i32 %0, i32* %2, align 4
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @char_func(i8 signext %0) #0 {
  %2 = alloca i8, align 1
  store i8 %0, i8* %2, align 1
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @local_array() #0 {
  %1 = alloca [10000 x i32], align 16
  %2 = alloca [0 x i32], align 4
  %3 = alloca [10 x i32], align 16
  %4 = alloca [1000 x i32], align 16
  %5 = alloca [10 x i32], align 16
  %6 = alloca [1000 x i32], align 16
  %7 = alloca [10 x i8], align 1
  %8 = alloca [1000 x i8], align 16
  %9 = alloca [10 x i8], align 1
  %10 = alloca [1000 x i8], align 16
  %11 = alloca [10 x i8], align 1
  %12 = alloca [1000 x i8], align 16
  %13 = alloca [10 x i8], align 1
  %14 = alloca [1000 x i8], align 16
  %15 = bitcast [10 x i32]* %3 to i8*
  call void @llvm.memset.p0i8.i64(i8* align 16 %15, i8 0, i64 40, i1 false)
  %16 = bitcast i8* %15 to <{ i32, i32, [8 x i32] }>*
  %17 = getelementptr inbounds <{ i32, i32, [8 x i32] }>, <{ i32, i32, [8 x i32] }>* %16, i32 0, i32 0
  store i32 1, i32* %17, align 16
  %18 = getelementptr inbounds <{ i32, i32, [8 x i32] }>, <{ i32, i32, [8 x i32] }>* %16, i32 0, i32 1
  store i32 2, i32* %18, align 4
  %19 = bitcast [1000 x i32]* %4 to i8*
  call void @llvm.memset.p0i8.i64(i8* align 16 %19, i8 0, i64 4000, i1 false)
  %20 = bitcast i8* %19 to <{ i32, i32, [998 x i32] }>*
  %21 = getelementptr inbounds <{ i32, i32, [998 x i32] }>, <{ i32, i32, [998 x i32] }>* %20, i32 0, i32 0
  store i32 1, i32* %21, align 16
  %22 = getelementptr inbounds <{ i32, i32, [998 x i32] }>, <{ i32, i32, [998 x i32] }>* %20, i32 0, i32 1
  store i32 2, i32* %22, align 4
  %23 = bitcast [10 x i32]* %5 to i8*
  call void @llvm.memset.p0i8.i64(i8* align 16 %23, i8 0, i64 40, i1 false)
  %24 = bitcast i8* %23 to <{ i32, i32, [8 x i32] }>*
  %25 = getelementptr inbounds <{ i32, i32, [8 x i32] }>, <{ i32, i32, [8 x i32] }>* %24, i32 0, i32 0
  store i32 1, i32* %25, align 16
  %26 = getelementptr inbounds <{ i32, i32, [8 x i32] }>, <{ i32, i32, [8 x i32] }>* %24, i32 0, i32 1
  store i32 2, i32* %26, align 4
  %27 = bitcast [1000 x i32]* %6 to i8*
  call void @llvm.memset.p0i8.i64(i8* align 16 %27, i8 0, i64 4000, i1 false)
  %28 = bitcast i8* %27 to <{ i32, i32, [998 x i32] }>*
  %29 = getelementptr inbounds <{ i32, i32, [998 x i32] }>, <{ i32, i32, [998 x i32] }>* %28, i32 0, i32 0
  store i32 1, i32* %29, align 16
  %30 = getelementptr inbounds <{ i32, i32, [998 x i32] }>, <{ i32, i32, [998 x i32] }>* %28, i32 0, i32 1
  store i32 2, i32* %30, align 4
  %31 = bitcast [10 x i8]* %7 to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* align 1 %31, i8* align 1 getelementptr inbounds (<{ i8, i8, [8 x i8] }>, <{ i8, i8, [8 x i8] }>* @__const.local_array.ca_local, i32 0, i32 0), i64 10, i1 false)
  %32 = bitcast [1000 x i8]* %8 to i8*
  call void @llvm.memset.p0i8.i64(i8* align 16 %32, i8 0, i64 1000, i1 false)
  %33 = bitcast i8* %32 to <{ i8, i8, [998 x i8] }>*
  %34 = getelementptr inbounds <{ i8, i8, [998 x i8] }>, <{ i8, i8, [998 x i8] }>* %33, i32 0, i32 0
  store i8 97, i8* %34, align 16
  %35 = getelementptr inbounds <{ i8, i8, [998 x i8] }>, <{ i8, i8, [998 x i8] }>* %33, i32 0, i32 1
  store i8 98, i8* %35, align 1
  %36 = bitcast [10 x i8]* %9 to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* align 1 %36, i8* align 1 getelementptr inbounds ([10 x i8], [10 x i8]* @__const.local_array.cas_local, i32 0, i32 0), i64 10, i1 false)
  %37 = bitcast [1000 x i8]* %10 to i8*
  call void @llvm.memset.p0i8.i64(i8* align 16 %37, i8 0, i64 1000, i1 false)
  %38 = bitcast i8* %37 to [1000 x i8]*
  %39 = getelementptr inbounds [1000 x i8], [1000 x i8]* %38, i32 0, i32 0
  store i8 97, i8* %39, align 16
  %40 = getelementptr inbounds [1000 x i8], [1000 x i8]* %38, i32 0, i32 1
  store i8 98, i8* %40, align 1
  %41 = bitcast [10 x i8]* %11 to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* align 1 %41, i8* align 1 getelementptr inbounds (<{ i8, i8, [8 x i8] }>, <{ i8, i8, [8 x i8] }>* @__const.local_array.cca_local, i32 0, i32 0), i64 10, i1 false)
  %42 = bitcast [1000 x i8]* %12 to i8*
  call void @llvm.memset.p0i8.i64(i8* align 16 %42, i8 0, i64 1000, i1 false)
  %43 = bitcast i8* %42 to <{ i8, i8, [998 x i8] }>*
  %44 = getelementptr inbounds <{ i8, i8, [998 x i8] }>, <{ i8, i8, [998 x i8] }>* %43, i32 0, i32 0
  store i8 97, i8* %44, align 16
  %45 = getelementptr inbounds <{ i8, i8, [998 x i8] }>, <{ i8, i8, [998 x i8] }>* %43, i32 0, i32 1
  store i8 98, i8* %45, align 1
  %46 = bitcast [10 x i8]* %13 to i8*
  call void @llvm.memcpy.p0i8.p0i8.i64(i8* align 1 %46, i8* align 1 getelementptr inbounds ([10 x i8], [10 x i8]* @__const.local_array.ccas_local, i32 0, i32 0), i64 10, i1 false)
  %47 = bitcast [1000 x i8]* %14 to i8*
  call void @llvm.memset.p0i8.i64(i8* align 16 %47, i8 0, i64 1000, i1 false)
  %48 = bitcast i8* %47 to [1000 x i8]*
  %49 = getelementptr inbounds [1000 x i8], [1000 x i8]* %48, i32 0, i32 0
  store i8 97, i8* %49, align 16
  %50 = getelementptr inbounds [1000 x i8], [1000 x i8]* %48, i32 0, i32 1
  store i8 98, i8* %50, align 1
  %51 = getelementptr inbounds [10 x i32], [10 x i32]* %3, i64 0, i64 0
  %52 = load i32, i32* %51, align 16
  call void @int_func(i32 %52)
  %53 = getelementptr inbounds [1000 x i32], [1000 x i32]* %4, i64 0, i64 0
  %54 = load i32, i32* %53, align 16
  call void @int_func(i32 %54)
  %55 = getelementptr inbounds [10 x i32], [10 x i32]* %3, i64 0, i64 2
  store i32 3, i32* %55, align 8
  %56 = getelementptr inbounds [1000 x i32], [1000 x i32]* %4, i64 0, i64 2
  store i32 3, i32* %56, align 8
  %57 = getelementptr inbounds [10 x i32], [10 x i32]* %5, i64 0, i64 0
  %58 = load i32, i32* %57, align 16
  call void @int_func(i32 %58)
  %59 = getelementptr inbounds [1000 x i32], [1000 x i32]* %6, i64 0, i64 0
  %60 = load i32, i32* %59, align 16
  call void @int_func(i32 %60)
  %61 = getelementptr inbounds [10 x i8], [10 x i8]* %7, i64 0, i64 0
  %62 = load i8, i8* %61, align 1
  call void @char_func(i8 signext %62)
  %63 = getelementptr inbounds [1000 x i8], [1000 x i8]* %8, i64 0, i64 0
  %64 = load i8, i8* %63, align 16
  call void @char_func(i8 signext %64)
  %65 = getelementptr inbounds [10 x i8], [10 x i8]* %9, i64 0, i64 0
  %66 = load i8, i8* %65, align 1
  call void @char_func(i8 signext %66)
  %67 = getelementptr inbounds [1000 x i8], [1000 x i8]* %10, i64 0, i64 0
  %68 = load i8, i8* %67, align 16
  call void @char_func(i8 signext %68)
  %69 = getelementptr inbounds [10 x i8], [10 x i8]* %7, i64 0, i64 2
  store i8 99, i8* %69, align 1
  %70 = getelementptr inbounds [1000 x i8], [1000 x i8]* %8, i64 0, i64 2
  store i8 99, i8* %70, align 2
  %71 = getelementptr inbounds [10 x i8], [10 x i8]* %9, i64 0, i64 2
  store i8 99, i8* %71, align 1
  %72 = getelementptr inbounds [1000 x i8], [1000 x i8]* %10, i64 0, i64 2
  store i8 99, i8* %72, align 2
  %73 = getelementptr inbounds [10 x i8], [10 x i8]* %11, i64 0, i64 0
  %74 = load i8, i8* %73, align 1
  call void @char_func(i8 signext %74)
  %75 = getelementptr inbounds [1000 x i8], [1000 x i8]* %12, i64 0, i64 0
  %76 = load i8, i8* %75, align 16
  call void @char_func(i8 signext %76)
  %77 = getelementptr inbounds [10 x i8], [10 x i8]* %13, i64 0, i64 0
  %78 = load i8, i8* %77, align 1
  call void @char_func(i8 signext %78)
  %79 = getelementptr inbounds [1000 x i8], [1000 x i8]* %14, i64 0, i64 0
  %80 = load i8, i8* %79, align 16
  call void @char_func(i8 signext %80)
  ret void
}

; Function Attrs: argmemonly nounwind willreturn
declare void @llvm.memset.p0i8.i64(i8* nocapture writeonly, i8, i64, i1 immarg) #1

; Function Attrs: argmemonly nounwind willreturn
declare void @llvm.memcpy.p0i8.p0i8.i64(i8* noalias nocapture writeonly, i8* noalias nocapture readonly, i64, i1 immarg) #1

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @main() #0 {
  %1 = alloca i32, align 4
  store i32 0, i32* %1, align 4
  %2 = load i32, i32* getelementptr inbounds ([10 x i32], [10 x i32]* bitcast (<{ i32, i32, [8 x i32] }>* @ia to [10 x i32]*), i64 0, i64 0), align 16
  call void @int_func(i32 %2)
  %3 = load i32, i32* getelementptr inbounds ([1000 x i32], [1000 x i32]* bitcast (<{ i32, i32, [998 x i32] }>* @lia to [1000 x i32]*), i64 0, i64 0), align 16
  call void @int_func(i32 %3)
  store i32 3, i32* getelementptr inbounds ([10 x i32], [10 x i32]* bitcast (<{ i32, i32, [8 x i32] }>* @ia to [10 x i32]*), i64 0, i64 2), align 8
  store i32 3, i32* getelementptr inbounds ([1000 x i32], [1000 x i32]* bitcast (<{ i32, i32, [998 x i32] }>* @lia to [1000 x i32]*), i64 0, i64 2), align 8
  %4 = load i32, i32* getelementptr inbounds ([10 x i32], [10 x i32]* bitcast (<{ i32, i32, [8 x i32] }>* @cia to [10 x i32]*), i64 0, i64 0), align 16
  call void @int_func(i32 %4)
  %5 = load i32, i32* getelementptr inbounds ([1000 x i32], [1000 x i32]* bitcast (<{ i32, i32, [998 x i32] }>* @lcia to [1000 x i32]*), i64 0, i64 0), align 16
  call void @int_func(i32 %5)
  %6 = load i8, i8* getelementptr inbounds ([10 x i8], [10 x i8]* bitcast (<{ i8, i8, [8 x i8] }>* @ca to [10 x i8]*), i64 0, i64 0), align 1
  call void @char_func(i8 signext %6)
  %7 = load i8, i8* getelementptr inbounds ([1000 x i8], [1000 x i8]* bitcast (<{ i8, i8, [998 x i8] }>* @lca to [1000 x i8]*), i64 0, i64 0), align 16
  call void @char_func(i8 signext %7)
  %8 = load i8, i8* getelementptr inbounds ([10 x i8], [10 x i8]* @cas, i64 0, i64 0), align 1
  call void @char_func(i8 signext %8)
  %9 = load i8, i8* getelementptr inbounds ([1000 x i8], [1000 x i8]* @lcas, i64 0, i64 0), align 16
  call void @char_func(i8 signext %9)
  store i8 99, i8* getelementptr inbounds ([10 x i8], [10 x i8]* bitcast (<{ i8, i8, [8 x i8] }>* @ca to [10 x i8]*), i64 0, i64 2), align 1
  store i8 99, i8* getelementptr inbounds ([1000 x i8], [1000 x i8]* bitcast (<{ i8, i8, [998 x i8] }>* @lca to [1000 x i8]*), i64 0, i64 2), align 2
  store i8 99, i8* getelementptr inbounds ([10 x i8], [10 x i8]* @cas, i64 0, i64 2), align 1
  store i8 99, i8* getelementptr inbounds ([1000 x i8], [1000 x i8]* @lcas, i64 0, i64 2), align 2
  %10 = load i8, i8* getelementptr inbounds ([10 x i8], [10 x i8]* bitcast (<{ i8, i8, [8 x i8] }>* @cca to [10 x i8]*), i64 0, i64 0), align 1
  call void @char_func(i8 signext %10)
  %11 = load i8, i8* getelementptr inbounds ([1000 x i8], [1000 x i8]* bitcast (<{ i8, i8, [998 x i8] }>* @lcca to [1000 x i8]*), i64 0, i64 0), align 16
  call void @char_func(i8 signext %11)
  %12 = load i8, i8* getelementptr inbounds ([10 x i8], [10 x i8]* @ccas, i64 0, i64 0), align 1
  call void @char_func(i8 signext %12)
  %13 = load i8, i8* getelementptr inbounds ([1000 x i8], [1000 x i8]* @lccas, i64 0, i64 0), align 16
  call void @char_func(i8 signext %13)
  ret i32 0
}

attributes #0 = { noinline nounwind optnone uwtable "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "frame-pointer"="all" "less-precise-fpmad"="false" "min-legal-vector-width"="0" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #1 = { argmemonly nounwind willreturn }

!llvm.module.flags = !{!0}
!llvm.ident = !{!1}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{!"clang version 10.0.0 "}
