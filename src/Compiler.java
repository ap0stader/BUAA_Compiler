import global.Config;
import frontend.lexer.TokenStream;
import frontend.lexer.Lexer;
import frontend.parser.CompUnit;
import frontend.error.ErrorTable;
import input.SourceCode;
import output.DumpAST;
import output.DumpErrorTable;
import output.DumpTokenStream;

import java.io.IOException;

import static java.lang.System.exit;

public class Compiler {
    public static void main(String[] args) {
        Config.setConfigByArgs(args);
        try {
            SourceCode sourceCode = new SourceCode(Config.inputFilename);
            frontend(sourceCode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==== 前端 ====
    private static void frontend(SourceCode sourceCode) throws IOException {
        // 创建前端所需的错误表
        ErrorTable errorTable = new ErrorTable();
        // Stage1 词法分析
        // 创建Lexer -> 得到TokenStream -> 关闭文件 -> 错误处理 -> [输出TokenStream]
        Lexer lexer = new Lexer(sourceCode.reader(), errorTable);
        TokenStream tokenStream = lexer.getTokenStream();
        sourceCode.close();
        // TODO 下一行为参与词法分析评测时的特地设置
        errorHandle(errorTable);
        if (Config.dumpTokenStream) {
            DumpTokenStream.dump(tokenStream.getArrayListCopy());
        }
        tryContinue(1);
        // Stage2 语法分析
        // 创建CompUnit(AST) -> 错误处理 -> [输出CompUnit(AST)]
        CompUnit compUnit = new CompUnit(tokenStream);
        errorHandle(errorTable);
        if (Config.dumpAST) {
            DumpAST.dump(compUnit);
        }
        tryContinue(2);
        // Stage3 语义分析
        //

        errorHandle(errorTable);

        tryContinue(3);
        return;
    }

    private static void errorHandle(ErrorTable errorTable) throws IOException {
        if (errorTable.notEmpty()) {
            DumpErrorTable.dump(errorTable);
            exit(1);
        }
    }

    private static void tryContinue(int nowStage) {
        if (Config.stages <= nowStage) {
            exit(0);
        }
    }
}