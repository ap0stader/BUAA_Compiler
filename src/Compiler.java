import IR.IRModule;
import frontend.visitor.Visitor;
import global.Config;
import frontend.lexer.TokenStream;
import frontend.lexer.Lexer;
import frontend.parser.CompUnit;
import frontend.error.ErrorTable;
import input.SourceCode;
import output.*;

import java.io.IOException;

import static java.lang.System.exit;

public class Compiler {
    public static void main(String[] args) {
        Config.setConfigByArgs(args);
        try {
            // 以源代码作为输入
            SourceCode sourceCode = new SourceCode(Config.inputFilename);
            // 前端：得到IRModule
            IRModule irModule = frontend(sourceCode);
            // 关闭打开的源代码文件
            sourceCode.close();
            // 优化前的LLVM
            if (Config.dumpLLVMBeforeOptimized) {
                DumpLLVM.dump(irModule);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==== 前端 ====
    private static IRModule frontend(SourceCode sourceCode) throws IOException {
        // 创建前端所需的错误表
        ErrorTable errorTable = new ErrorTable();
        // Stage1 词法分析
        // 创建Lexer -> 得到TokenStream -> [输出TokenStream]
        Lexer lexer = new Lexer(sourceCode.reader(), errorTable);
        TokenStream tokenStream = lexer.getTokenStream();
        errorHandle(1, errorTable);
        if (Config.dumpTokenStream) {
            DumpTokenStream.dump(tokenStream.getArrayList());
        }
        tryContinue(1);
        // Stage2 语法分析
        // 创建CompUnit(AST) -> [输出CompUnit(AST)]
        CompUnit compUnit = new CompUnit(tokenStream);
        errorHandle(2, errorTable);
        if (Config.dumpAST) {
            DumpAST.dump(compUnit);
        }
        tryContinue(2);
        // Stage3 语义分析
        // 创建Visitor -> 得到Module -> 错误处理 -> [输出符号表]
        Visitor visitor = new Visitor(compUnit, errorTable);
        IRModule irModule = visitor.visitCompUnit();
        errorHandle(3, errorTable);
        if (Config.dumpSymbolTable) {
            DumpSymbolTable.dump(visitor.getSymbolTable());
        }
        tryContinue(3);
        if (errorTable.notEmpty()) {
            DumpErrorTable.dump(errorTable);
            exit(1);
        }
        return irModule;
    }

    private static void errorHandle(int nowStage, ErrorTable errorTable) throws IOException {
        if (nowStage >= Config.stages && errorTable.notEmpty()) {
            DumpErrorTable.dump(errorTable);
            exit(1);
        }
    }

    private static void tryContinue(int nowStage) {
        if (nowStage >= Config.stages) {
            exit(0);
        }
    }
}
