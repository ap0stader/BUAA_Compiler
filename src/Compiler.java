import config.Config;
import frontend.lexer.TokenStream;
import frontend.lexer.Lexer;
import util.dump.DumpTokenList;

import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;

import static java.lang.System.exit;

public class Compiler {
    public static void main(String[] args) {
        Config.setConfigByArgs(args);
        try {
            // Stage1 词法分析
            // 打开文件 -> 创建Lexer -> 得到TokenList -> 关闭文件 -> [输出TokenList] -> 尝试继续
            PushbackReader inputFileReader = new PushbackReader(new FileReader(Config.inputFilename));
            Lexer lexer = new Lexer(inputFileReader);
            TokenStream tokenStream = lexer.generateTokenStream();
            inputFileReader.close();
            if (Config.dumpTokenList) {
                DumpTokenList.dump(tokenStream.getArrayListCopy());
            }
            tryContinue();
            // Stage2 语法分析
            // TODO
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 比对需要执行步骤与当前已经执行了的步骤，判断是否应当继续执行
    private static int finishedStep = 0;

    private static void tryContinue() {
        finishedStep++;
        if (finishedStep >= Config.stages) {
            exit(0);
        }
    }
}