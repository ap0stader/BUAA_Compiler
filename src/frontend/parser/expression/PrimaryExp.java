package frontend.parser.expression;

import global.Config;
import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.ASTNodeOption;
import frontend.type.ASTNodeWithOption;
import frontend.type.TokenType;
import frontend.error.ErrorType;

import java.util.ArrayList;

public class PrimaryExp extends ASTNodeWithOption<PrimaryExp.PrimaryExpOption> {
    private PrimaryExp(PrimaryExpOption option) {
        super(option);
    }

    public interface PrimaryExpOption extends ASTNodeOption {
    }

    // PrimaryExp → '(' Exp ')' | LVal | Number | Character
    //                     LVal → Ident {'[' Exp ']'}
    //                          Number → IntConst
    //                                Character → CharConst
    static PrimaryExp parse(TokenStream stream) {
        if (stream.isNow(TokenType.LPARENT)) {
            return new PrimaryExp(new PrimaryExp_Exp(stream));
        } else if (stream.isNow(TokenType.IDENFR)) {
            return new PrimaryExp(new PrimaryExp_LVal(stream));
        } else if (stream.isNow(TokenType.INTCON)) {
            return new PrimaryExp(new PrimaryExp_Number(stream));
        } else if (stream.isNow(TokenType.CHRCON)) {
            return new PrimaryExp(new PrimaryExp_Character(stream));
        } else {
            if (Config.parserThrowable) {
                throw new RuntimeException("When PrimaryExp.parse(), unexpected token: " + stream.getNow());
            } else {
                return null;
            }
        }
    }

    // PrimaryExp → '(' Exp ')'
    public static class PrimaryExp_Exp implements PrimaryExpOption {
        private final Token lparentToken;
        private final Exp exp;
        private final Token rparentToken;

        private PrimaryExp_Exp(TokenStream stream) {
            String place = "PrimaryExp_Exp()";
            lparentToken = stream.consumeOrThrow(place, TokenType.LPARENT);
            exp = new Exp(stream);
            rparentToken = stream.consumeOrError(place, ErrorType.MISSING_RPARENT, TokenType.RPARENT);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(lparentToken);
            ret.add(exp);
            ret.add(rparentToken);
            return ret;
        }

        public Exp exp() {
            return exp;
        }
    }

    // PrimaryExp → LVal
    public static class PrimaryExp_LVal implements PrimaryExpOption {
        private final LVal lval;

        private PrimaryExp_LVal(TokenStream stream) {
            lval = new LVal(stream);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(lval);
            return ret;
        }

        public LVal lval() {
            return lval;
        }
    }

    // PrimaryExp → Number
    public static class PrimaryExp_Number implements PrimaryExpOption {
        private final Number number;

        private PrimaryExp_Number(TokenStream stream) {
            number = new Number(stream);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(number);
            return ret;
        }

        public Number number() {
            return number;
        }
    }

    public static class PrimaryExp_Character implements PrimaryExpOption {
        private final Character character;

        private PrimaryExp_Character(TokenStream stream) {
            character = new Character(stream);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(character);
            return ret;
        }

        public Character character() {
            return character;
        }
    }
}
