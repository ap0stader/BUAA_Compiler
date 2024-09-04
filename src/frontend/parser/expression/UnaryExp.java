package frontend.parser.expression;

import config.Config;
import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.ASTNodeOption;
import frontend.type.ASTNodeWithOption;
import frontend.type.TokenType;

import java.util.ArrayList;

public class UnaryExp extends ASTNodeWithOption<UnaryExp.UnaryExpOption> {
    private UnaryExp(UnaryExpOption option) {
        super(option);
    }

    public interface UnaryExpOption extends ASTNodeOption {
    }

    // UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    // PrimaryExp → '(' Exp ')' | LVal | Number
    //                     LVal → Ident {'[' Exp ']'}
    //                          Number → IntConst
    // UnaryOp → '+' | '−' | '!'
    public static UnaryExp parse(TokenStream stream) {
        if (stream.isNow(TokenType.PLUS, TokenType.MINU, TokenType.NOT)) {
            return new UnaryExp(new UnaryExp_UnaryOp(stream));
        } else if (stream.isNow(TokenType.IDENFR) && stream.isNext(1, TokenType.LPARENT)) {
            return new UnaryExp(new UnaryExp_IndetFuncCall(stream));
        } else if (stream.isNow(TokenType.LPARENT, TokenType.IDENFR, TokenType.INTCON)) {
            return new UnaryExp(new UnaryExp_PrimaryExp(stream));
        } else {
            if (Config.parserThrowable) {
                throw new RuntimeException("When UnaryExp.parse(), unexpected token: " + stream.getNow());
            } else {
                return null;
            }
        }
    }

    // UnaryExp → PrimaryExp
    public static class UnaryExp_PrimaryExp implements UnaryExpOption {
        private final PrimaryExp primaryExp;

        public UnaryExp_PrimaryExp(TokenStream stream) {
            primaryExp = PrimaryExp.parse(stream);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(primaryExp);
            return ret;
        }

        public PrimaryExp primaryExp() {
            return primaryExp;
        }
    }

    // UnaryExp → Ident '(' [FuncRParams] ')'
    public static class UnaryExp_IndetFuncCall implements UnaryExpOption {
        private final Token ident;
        private final Token lbrackToken;
        private final FuncRParams funcRParams;
        private final Token rbrackToken;

        public UnaryExp_IndetFuncCall(TokenStream stream) {
            String place = "UnaryExp_IndetFuncCall()";
            ident = stream.consumeOrThrow(place, TokenType.IDENFR);
            lbrackToken = stream.consumeOrThrow(place, TokenType.LPARENT);
            funcRParams = new FuncRParams(stream);
            rbrackToken = stream.consumeOrThrow(place, TokenType.RPARENT);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(ident);
            ret.add(lbrackToken);
            if (funcRParams != null) {
                ret.add(funcRParams);
            }
            ret.add(rbrackToken);
            return ret;
        }

        public Token ident() {
            return ident;
        }

        public FuncRParams funcRParams() {
            return funcRParams;
        }
    }

    // UnaryExp → UnaryOp UnaryExp
    public static class UnaryExp_UnaryOp implements UnaryExpOption {
        private final UnaryOp unaryOp;
        private final UnaryExp unaryExp;

        public UnaryExp_UnaryOp(TokenStream stream) {
            unaryOp = new UnaryOp(stream);
            unaryExp = UnaryExp.parse(stream);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(unaryOp);
            ret.add(unaryExp);
            return ret;
        }

        public UnaryOp unaryOp() {
            return unaryOp;
        }

        public UnaryExp unaryExp() {
            return unaryExp;
        }
    }
}
