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
            return new UnaryExp(new UnaryExp_UnaryOP(stream));
        } else if (stream.isNow(TokenType.IDENFR) && stream.isNext(1, TokenType.LPARENT)) {
            return new UnaryExp(new UnaryExp_FuncIndet(stream));
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
            primaryExp = new PrimaryExp(stream);
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
    public static class UnaryExp_FuncIndet implements UnaryExpOption {
        private final Token ident;
        private final Token lbrackToken;
        private final FuncRParams funcRParams;
        private final Token rbrackToken;

        public UnaryExp_FuncIndet(TokenStream stream) {
            String place = "UnaryExp_FuncIndet()";
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
    public static class UnaryExp_UnaryOP implements UnaryExpOption {
        private final UnaryOP unaryOP;
        private final UnaryExp unaryExp;

        public UnaryExp_UnaryOP(TokenStream stream) {
            unaryOP = new UnaryOP(stream);
            unaryExp = UnaryExp.parse(stream);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(unaryOP);
            ret.add(unaryExp);
            return ret;
        }

        public UnaryOP unaryOP() {
            return unaryOP;
        }

        public UnaryExp unaryExp() {
            return unaryExp;
        }
    }
}
