package global;

public class Config {
    // 输入文件名
    public static final String inputFilename = "testfile.txt";
    // 错误输出文件名
    public static final String dumpErrorTableFileName = "error.txt";

    // 是否输出错误详情
    public static boolean dumpErrorTableDetail = false;

    // 前端遇到未定义的异常情况，是否抛出运行时异常
    // 词法分析，如果不允许抛出异常，默认处理方式为ungetc();或无操作
    public static boolean lexerThrowable = true;
    // 语法分析，如果不允许抛出异常，默认处理方式为return null;或无操作
    public static boolean parserThrowable = true;
    // 语义分析，如果不允许抛出异常，默认处理方式为return null;或无操作
    public static boolean visitorThrowable = true;

    // 生成LLVM IR时，是否禁止对长数组进行优化
    public static final boolean disableLongArrayOptimization = false;

    // 输出LLVM时，是否输出debug信息
    public static final boolean dumpLLVMDetail = false;

    // 是否开启中端优化
    public static boolean enableMiddleOptimization = true;

    // 是否开启后端优化
    public static boolean enableBackendOptimization = true;

    /* 总共进行阶段数
       ==== 前端 ====
       1. 词法分析，生成TokenStream
       2. 语法分析，生成CompUnit(AST)
       3. 语义分析，生成IRModule
       ==== 中端 ====
       4. 中端优化
       ==== 后端 ====
       5. 生成目标代码
     */
    public final static int maxStages = 5;
    public static int stages = 5;

    // 词法分析后，是否输出TokenStream、输出的文件名、是否输出行号等信息
    public static boolean dumpTokenStream = false;
    public static String dumpTokenStreamFileName = "lexer.txt";
    public static boolean dumpTokenStreamLineNumber = false;

    // 语法分析后，是否输出CompUnit(AST)、输出的文件名
    public static boolean dumpAST = false;
    public static String dumpASTFileName = "parser.txt";

    // 语义分析后，是否输出SymbolTable、输出的文件名，是否输出详情信息
    public static boolean dumpSymbolTable = false;
    public static String dumpSymbolTableFileName = "symbol.txt";
    public static boolean dumpSymbolTableDetail = false;

    // 中端优化前，是否输出优化前的LLVM
    public static boolean dumpLLVMBeforeOptimized = true;
    public static String dumpLLVMBeforeOptimizedFileName = "llvm_ir_before.txt";

    // 中端优化后，是否输出优化后的LLVM
    public static boolean dumpLLVMAfterOptimized = true;
    public static String dumpLLVMAfterOptimizedFileName = "llvm_ir.txt";

    // 生成目标代码后，是否输出未进行寄存器分配的目标代码
    public static boolean dumpMIPSAssemblyBeforeAllocation = true;
    public static String dumpMIPSAssemblyBeforeAllocationFileName = "mips_before.txt";

    // 生成目标代码后，是否输出已进行寄存器分配的目标代码
    public static boolean dumpMIPSAssemblyAfterAllocation = true;
    public static String dumpMIPSAssemblyAfterAllocationFileName = "mips.txt";

    // 通过传递的参数设置全局配置
    public static void setConfigByArgs(String[] args) {
        for (String arg : args) {
            switch (arg) {
                // 前端抛出异常限制
                case "--no-all-throw" -> {
                    lexerThrowable = false;
                    parserThrowable = false;
                    visitorThrowable = false;
                }
                case "--no-lexer-throw" -> lexerThrowable = false;
                case "--no-parser-throw" -> parserThrowable = false;
                case "--no-visitor-throw" -> visitorThrowable = false;
                // 调试模式
                case "--debug" -> {
                    stages = maxStages;
                    dumpErrorTableDetail = true;
                    dumpTokenStream = true;
                    dumpTokenStreamFileName = "dump_TokenStream.txt";
                    dumpTokenStreamLineNumber = true;
                    dumpAST = true;
                    dumpASTFileName = "dump_AST.txt";
                    dumpSymbolTable = true;
                    dumpSymbolTableFileName = "dump_SymbolTable.txt";
                    dumpSymbolTableDetail = true;
                    dumpLLVMBeforeOptimized = true;
                    dumpLLVMBeforeOptimizedFileName = "dump_LLVMBeforeOptimized.txt";
                    dumpLLVMAfterOptimized = true;
                    dumpLLVMAfterOptimizedFileName = "dump_LLVMAfterOptimized.txt";
                    dumpMIPSAssemblyBeforeAllocation = true;
                    dumpMIPSAssemblyBeforeAllocationFileName = "dump_MIPSAssemblyBeforeAllocation.txt";
                    dumpMIPSAssemblyAfterAllocation = true;
                    dumpMIPSAssemblyAfterAllocationFileName = "dump_MIPSAssemblyAfterAllocation.txt";
                }
                // Lexical Analysis: -L
                case "-L" -> {
                    stages = 1;
                    dumpTokenStream = true;
                }
                // Syntax Analysis: -S
                case "-S" -> {
                    stages = 2;
                    dumpAST = true;
                }
                // Semantic Analysis: -E
                case "-E" -> {
                    stages = 3;
                    dumpSymbolTable = true;
                }
                // Code Generation(LLVM IR): -L
                case "-I" -> {
                    stages = 4;
                    dumpLLVMBeforeOptimized = false;
                    enableMiddleOptimization = false;
                    dumpLLVMAfterOptimized = true;
                }
                // Code Generation(MIPS): -M
                case "-M" -> {
                    stages = 5;
                    dumpLLVMBeforeOptimized = false;
                    enableMiddleOptimization = false;
                    dumpLLVMAfterOptimized = true;
                    enableBackendOptimization = false;
                    dumpMIPSAssemblyBeforeAllocation = true;
                    dumpMIPSAssemblyAfterAllocation = true;
                }
                // Code Generation(MIPS, optimized): -O
                case "-O" -> {
                    stages = 5;
                    dumpLLVMBeforeOptimized = true;
                    enableMiddleOptimization = true;
                    dumpLLVMAfterOptimized = true;
                    enableBackendOptimization = true;
                    dumpMIPSAssemblyBeforeAllocation = true;
                    dumpMIPSAssemblyAfterAllocation = true;
                }
            }
        }
    }

    private Config() {
    }
}
