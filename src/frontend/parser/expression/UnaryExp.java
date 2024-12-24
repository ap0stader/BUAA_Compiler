package frontend.parser.expression;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.*;

import java.util.ArrayList;

public class UnaryExp extends ASTNodeWithOption<UnaryExp.UnaryExpOption> {
    private UnaryExp(UnaryExpOption option) {
        super(option);
    }

    public interface UnaryExpOption extends ASTNodeOption {
    }

    // UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    // PrimaryExp → '(' Exp ')' | LVal | Number | Character
    //                     LVal → Ident {'[' Exp ']'}
    //                          Number → IntConst
    //                                Character → CharConst
    // UnaryOp → '+' | '−' | '!'
    static UnaryExp parse(TokenStream stream) {
        if (stream.isNow(TokenType.PLUS, TokenType.MINU, TokenType.NOT)) {
            return new UnaryExp(new UnaryExp_UnaryOp(stream));
        } else if (stream.isNow(TokenType.IDENFR) && stream.isNext(1, TokenType.LPARENT)) {
            return new UnaryExp(new UnaryExp_IdentFuncCall(stream));
        } else if (stream.isNow(TokenType.LPARENT, TokenType.IDENFR, TokenType.INTCON, TokenType.CHRCON)) {
            return new UnaryExp(new UnaryExp_PrimaryExp(stream));
        } else {
            throw new CatchableUnexpectedToken("When UnaryExp.parse(), unexpected token: " + stream.getNow());
        }
    }

    // UnaryExp → PrimaryExp
    public static class UnaryExp_PrimaryExp implements UnaryExpOption {
        private final PrimaryExp primaryExp;

        private UnaryExp_PrimaryExp(TokenStream stream) {
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
    public static class UnaryExp_IdentFuncCall implements UnaryExpOption {
        private final Token ident;
        private final Token lparentToken;
        private final FuncRParams funcRParams;
        private final Token rparentToken;

        private UnaryExp_IdentFuncCall(TokenStream stream) {
            String place = "UnaryExp_IdentFuncCall()";
            ident = stream.consumeOrThrow(place, TokenType.IDENFR);
            lparentToken = stream.consumeOrThrow(place, TokenType.LPARENT);
            // FuncRParams → Exp { ',' Exp }
            FuncRParams tryfuncRParams;
            int checkpointID = stream.checkpoint("IdentFuncCallTry");
            try {
                new Exp(stream);
                stream.restore(checkpointID);
                tryfuncRParams = new FuncRParams(stream);
            } catch (CatchableUnexpectedToken e) {
                // 尝试读取一次Exp，如果有RuntimeError并且和checkpoint的比较偏移为0，说明没有Exp，也就是没有funcRParams
                if (stream.offset(checkpointID) == 0) {
                    tryfuncRParams = null;
                } else {
                    throw e;
                }
            }
            funcRParams = tryfuncRParams;
            rparentToken = stream.consumeOrError(place, ErrorType.MISSING_RPARENT, TokenType.RPARENT);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(ident);
            ret.add(lparentToken);
            if (funcRParams != null) {
                ret.add(funcRParams);
            }
            ret.add(rparentToken);
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

        private UnaryExp_UnaryOp(TokenStream stream) {
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
