package global;

public class Config {
    // 输入文件名
    public static final String inputFilename = "testfile.txt";
    // 最终输出文件名
    public static final String outputFilename = "output.txt";

    // 遇到未定义的异常情况，是否抛出运行时错误
    // 词法分析，如果不允许抛出错误，默认处理方式为ungetc();或无操作
    public static boolean lexerThrowable = true;
    // 语法分析，如果不允许抛出错误，默认处理方式为return null;
    public static boolean parserThrowable = true;

    // 前端完成后存在错误，是否终止
    public static boolean frontendErrorInterrupt = true;

    /* 总共进行阶段数
       ==== 前端 ====
       1. 词法分析，生成TokenStream
       2. 语法分析，生成CompUnit(AST)
       3. 语义分析，生成IR(LLVM)
     */
    public static int stages = 3;

    // 词法分析后，是否输出TokenStream、输出的文件名、是否输出行号等信息
    public static boolean dumpTokenStream = false;
    public static String dumpTokenStreamFileName = outputFilename;
    public static boolean dumpTokenStreamLineNumber = false;

    // 语法分析后，是否输出CompUnit(AST)、输出的文件名
    public static boolean dumpAST = false;
    public static String dumpASTFileName = outputFilename;

    // 语义分析后，是否输出错误收集结果、输出的文件名、是否输出错误详情
    public static boolean dumpErrorTable = false;
    public static String dumpErrorTableFileName = outputFilename;
    public static boolean dumpErrorTableDetail = false;

    // 通过传递的参数设置全局配置
    public static void setConfigByArgs(String[] args) {
        for (String arg : args) {
            switch (arg) {
                // 抛出异常限制
                case "--no-all-throw" -> {
                    lexerThrowable = false;
                    parserThrowable = false;
                }
                case "--no-lexer-throw" -> lexerThrowable = false;
                case "--no-parser-throw" -> parserThrowable = false;
                case "--disable-frontend-error-interrupt" -> frontendErrorInterrupt = false;
                // 调试模式
                case "--debug" -> {
                    dumpTokenStream = true;
                    dumpTokenStreamFileName = "dump_TokenStream.txt";
                    dumpTokenStreamLineNumber = true;
                    dumpAST = true;
                    dumpASTFileName = "dump_AST.txt";
                    dumpErrorTable = true;
                    dumpErrorTableFileName = "dump_ErrorTable.txt";
                    dumpErrorTableDetail = true;
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
                    dumpErrorTable = true;
                }
            }
        }
    }

    private Config() {
    }
}
