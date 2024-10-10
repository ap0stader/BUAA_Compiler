import IR.IRModule;
import frontend.visitor.Vistor;
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
        if (Config.dumpTokenStream) {
            DumpTokenStream.dump(tokenStream.getArrayList());
        }
        tryContinue(1);
        // Stage2 语法分析
        // 创建CompUnit(AST) -> [输出CompUnit(AST)]
        CompUnit compUnit = new CompUnit(tokenStream);
        if (Config.dumpAST) {
            DumpAST.dump(compUnit);
        }
        tryContinue(2);
        // Stage3 语义分析
        // 创建Visitor -> 得到Module -> 错误处理 -> [输出符号表]
        Vistor vistor = new Vistor(compUnit, errorTable);
        IRModule irModule = vistor.visitCompUnit();
        errorHandle(errorTable);
        if (Config.dumpSymbolTable) {
            DumpSymbolTable.dump(vistor.getSymbolTable());
        }
        tryContinue(3);
        return irModule;
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