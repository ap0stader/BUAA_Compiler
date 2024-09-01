package config;

public class Config {
    // 输入文件名
    public static String inputFilename = "testfile.txt";
    // 最终输出文件名
    public static String outputFilename = "output.txt";

    /* 总共进行阶段数
       1. 词法分析，生成TokenList
       2. 语法分析，生成AST
     */
    public static int stages = 2;

    // 词法分析后，是否输出TokenStream及其输出的文件名
    public static boolean dumpTokenList = false;
    public static String dumpTokenListFileName = outputFilename;

    // 语法分析后，是否输出AST
    public static boolean dumpAST = true;
    public static String dumpASTFileName = outputFilename;

    // 通过传递的参数设置全局配置
    public static void setConfigByArgs(String[] args) {
        for (String arg : args) {
            switch (arg) {
                // Lexical Analysis: -L
                case "-L":
                    stages = 1;
                    dumpTokenList = true;
                    break;
                // Syntax Analysis: -S
                case "-S":
                    stages = 2;
                    dumpAST = true;
                    break;
                default:
                    break;
            }
        }
    }
}
