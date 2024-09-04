package config;

public class Config {
    // 输入文件名
    public static final String inputFilename = "testfile.txt";
    // 最终输出文件名
    public static final String outputFilename = "output.txt";

    // 遇到未定义的异常情况，抛出运行时错误，在非特殊
    // 词法分析，如果不允许抛出错误，默认处理方式为ungetc();或无操作
    public static boolean lexerThrowable = true;
    // 语法分析，如果不允许抛出错误，默认处理方式为return null;
    public static boolean parserThrowable = true;

    /* 总共进行阶段数
       1. 词法分析，生成TokenList
       2. 语法分析，生成AST
     */
    public static int stages = 2;

    // 词法分析后，是否输出TokenStream、输出的文件名是否输出行号等信息
    public static boolean dumpTokenList = false;
    public static final String dumpTokenListFileName = "dump_tokenlist.txt";
    public static final boolean dumpTokenListLineNumber = true;

    // 语法分析后，是否输出AST、输出的文件名
    public static boolean dumpAST = true;
    public static final String dumpASTFileName = outputFilename;

    // 通过传递的参数设置全局配置
    public static void setConfigByArgs(String[] args) {
        for (String arg : args) {
            switch (arg) {
                // 抛出错误限制
                case "--no-all-throw" -> {
                    lexerThrowable = false;
                    parserThrowable = false;
                }
                case "--no-lexer-throw" -> lexerThrowable = false;
                case "--no-parser-throw" -> parserThrowable = false;
                // Lexical Analysis: -L
                case "-L" -> {
                    stages = 1;
                    dumpTokenList = true;
                }
                // Syntax Analysis: -S
                case "-S" -> {
                    stages = 2;
                    dumpAST = true;
                }
            }
        }
    }
}
