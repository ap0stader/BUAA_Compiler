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

    /* 总共进行阶段数
       ==== 前端 ====
       1. 词法分析，生成TokenStream
       2. 语法分析，生成CompUnit(AST)
       3. 语义分析，生成IRModule
     */
    public final static int maxStages = 4;
    public static int stages = 4;

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
    public static String dumpLLVMBeforeOptimizedFileName = "llvm_ir.txt";

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
                    dumpLLVMBeforeOptimized = true;
                }
            }
        }
    }

    private Config() {
    }
}
