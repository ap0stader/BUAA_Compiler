package util;

import global.Config;
import frontend.lexer.Token;
import frontend.parser.CompUnit;
import frontend.type.ASTNode;
import frontend.type.ASTNodeWithOption;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class DumpAST {
    private static BufferedWriter out;

    public static void dump(CompUnit compUnit) throws IOException {
        out = new BufferedWriter(new FileWriter(Config.dumpASTFileName));
        dump(compUnit.explore());
        out.write("<CompUnit>");
        out.close();
    }

    // 语法分析成分分析结束前，另起一行输出当前语法成分的名字
    // <BlockItem>, <Decl>, <BType> 不用输出
    private static void dump(ArrayList<Object> nodes) throws IOException {
        for (Object node : nodes) {
            if (node instanceof ASTNode branchNode) {
                dump(branchNode.explore());
                out.write("<" + branchNode.getClass().getSimpleName() + ">" + "\n");
            } else if (node instanceof ASTNodeWithOption<?> branchNodeWithOption) {
                dump(branchNodeWithOption.extract().explore());
                out.write("<" + branchNodeWithOption.getClass().getSimpleName() + ">" + "\n");
            } else if (node instanceof Token leafNode) {
                out.write(leafNode.type().toString() + " " + leafNode.strVal() + "\n");
            }
        }
    }

    private DumpAST() {
    }
}
