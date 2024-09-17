import global.Config;
import frontend.lexer.TokenStream;
import frontend.lexer.Lexer;
import frontend.parser.CompUnit;
import global.error.ErrorTable;
import util.DumpAST;
import util.DumpErrorTable;
import util.DumpTokenStream;

import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;

public class Compiler {
    public static void main(String[] args) {
        Config.setConfigByArgs(args);
        try {
            frontend();
            DumpErrorTable.dump();
            if (Config.stages >= 4 && ErrorTable.isEmpty()) {

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==== 前端 ====
    private static void frontend() throws IOException {
        // Stage1 词法分析
        // 打开文件 -> 创建Lexer -> 得到TokenStream -> 关闭文件 -> [输出TokenStream] -> 尝试继续
        PushbackReader inputFileReader = new PushbackReader(new FileReader(Config.inputFilename));
        Lexer lexer = new Lexer(inputFileReader);
        TokenStream tokenStream = lexer.getTokenStream();
        inputFileReader.close();
        if (Config.dumpTokenStream) {
            DumpTokenStream.dump(tokenStream.getArrayListCopy());
        }
        if (Config.stages <= 1) {
            return;
        }
        // Stage2 语法分析
        // 创建CompUnit(AST) -> [输出CompUnit(AST)] -> 尝试继续
        CompUnit compUnit = new CompUnit(tokenStream);
        if (Config.dumpAST) {
            DumpAST.dump(compUnit);
        }
        if (Config.stages <= 2) {
            return;
        }
        // Stage3 语义分析
        //
    }
}