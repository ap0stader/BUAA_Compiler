package frontend.parser.statement;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.parser.expression.Cond;
import frontend.parser.expression.Exp;
import frontend.parser.expression.LVal;
import frontend.type.ASTNodeOption;
import frontend.type.ASTNodeWithOption;
import frontend.type.TokenType;

import java.util.ArrayList;

public class Stmt extends ASTNodeWithOption<Stmt.StmtOption> implements BlockItem {
    private Stmt(StmtOption option) {
        super(option);
    }

    public interface StmtOption extends ASTNodeOption {
    }

    /* Stmt → LVal '=' Exp ';'
            | [Exp] ';'
            | Block
      Block → '{' { BlockItem } '}'
            | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
            | 'break' ';'
            | 'continue' ';'
            | 'return' [Exp] ';'
            | LVal '=' 'getint' '(' ')' ';'
            | 'printf' '(' FormatString { ',' Exp } ')' ';' */
    public static Stmt parse(TokenStream stream) {
        return switch (stream.getNow().type()) {
            case SEMICN -> new Stmt(new Stmt_Semicn(stream));
            case LBRACE -> new Stmt(new Stmt_Block(stream));
            case IFTK -> new Stmt(new Stmt_If(stream));
            case FORTK -> new Stmt(new Stmt_For(stream));
            case BREAKTK -> new Stmt(new Stmt_Break(stream));
            case CONTINUETK -> new Stmt(new Stmt_Continue(stream));
            case RETURNTK -> new Stmt(new Stmt_Return(stream));
            case PRINTFTK -> new Stmt(new Stmt_Printf(stream));
            default -> {
                // Stmt → LVal '=' Exp ';'
                //      | Exp ';'
                //      | LVal '=' 'getint' '(' ')' ';'
                stream.checkpoint("StmtTry");
                Exp tryExp = new Exp(stream);
                if (stream.isNow(TokenType.ASSIGN)) {
                    stream.restore("StmtTry");
                    if (stream.isNext(1, TokenType.GETINTTK)) {
                        yield new Stmt(new Stmt_LValGetint(stream));
                    } else {
                        yield new Stmt(new Stmt_LValAssign(stream));
                    }
                } else {
                    yield new Stmt(new Stmt_Exp(tryExp, stream));
                }
            }
        };
    }

    // Stmt → LVal '=' Exp ';'
    public static class Stmt_LValAssign implements StmtOption {
        private final LVal lval;
        private final Token assignToken;
        private final Exp exp;
        private final Token semicnToken;

        public Stmt_LValAssign(TokenStream stream) {
            String place = "Stmt_LValAssign()";
            lval = new LVal(stream);
            assignToken = stream.consumeOrThrow(place, TokenType.ASSIGN);
            exp = new Exp(stream);
            semicnToken = stream.consumeOrThrow(place, TokenType.SEMICN);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(lval);
            ret.add(assignToken);
            ret.add(exp);
            ret.add(semicnToken);
            return ret;
        }

        public LVal lval() {
            return lval;
        }

        public Exp exp() {
            return exp;
        }
    }

    // Stmt → ';'
    public static class Stmt_Semicn implements StmtOption {
        private final Token semicnToken;

        public Stmt_Semicn(TokenStream stream) {
            String place = "Stmt_Semicn()";
            semicnToken = stream.consumeOrThrow(place, TokenType.SEMICN);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(semicnToken);
            return ret;
        }
    }

    // Stmt → [Exp] ';'
    public static class Stmt_Exp implements StmtOption {
        private final Exp exp;
        private final Token semicnToken;

        public Stmt_Exp(Exp exp, TokenStream stream) {
            String place = "Stmt_Exp()";
            this.exp = exp;
            semicnToken = stream.consumeOrThrow(place, TokenType.SEMICN);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(exp);
            ret.add(semicnToken);
            return ret;
        }

        public Exp exp() {
            return exp;
        }
    }

    // Stmt → Block
    public static class Stmt_Block implements StmtOption {
        private final Block block;

        public Stmt_Block(TokenStream stream) {
            String place = "Stmt_Block()";
            block = new Block(stream);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(block);
            return ret;
        }

        public Block block() {
            return block;
        }
    }

    // Stmt → 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    public static class Stmt_If implements StmtOption {
        private final Token ifToken;
        private final Token lparentToken;
        private final Cond cond;
        private final Token rparentToken;
        private final Stmt ifStmt;
        private final Token elseToken;
        private final Stmt elseStmt;

        public Stmt_If(TokenStream stream) {
            String place = "Stmt_If()";
            ifToken = stream.consumeOrThrow(place, TokenType.IFTK);
            lparentToken = stream.consumeOrThrow(place, TokenType.LPARENT);
            cond = new Cond(stream);
            rparentToken = stream.consumeOrThrow(place, TokenType.RPARENT);
            ifStmt = Stmt.parse(stream);
            elseToken = stream.consumeOrNull(TokenType.ELSETK);
            elseStmt = elseToken != null ? Stmt.parse(stream) : null;
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(ifToken);
            ret.add(lparentToken);
            ret.add(cond);
            ret.add(rparentToken);
            ret.add(ifStmt);
            if (elseToken != null) {
                ret.add(elseToken);
                ret.add(elseStmt);
            }
            return ret;
        }

        public Cond cond() {
            return cond;
        }

        public Stmt ifStmt() {
            return ifStmt;
        }

        public Token elseToken() {
            return elseToken;
        }

        public Stmt elseStmt() {
            return elseStmt;
        }


    }

    // Stmt → 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    public static class Stmt_For implements StmtOption {
        private final Token forToken;
        private final Token lparentToken;
        private final ForStmt initForStmt;
        private final Token fisrtSemicnToken;
        private final Cond cond;
        private final Token secondSemicnToken;
        private final ForStmt afterForStmt;
        private final Token rparentToken;
        private final Stmt stmt;

        public Stmt_For(TokenStream stream) {
            String place = "Stmt_For()";
            forToken = stream.consumeOrThrow(place, TokenType.FORTK);
            lparentToken = stream.consumeOrThrow(place, TokenType.LPARENT);
            initForStmt = new ForStmt(stream);
            fisrtSemicnToken = stream.consumeOrThrow(place, TokenType.SEMICN);
            cond = new Cond(stream);
            secondSemicnToken = stream.consumeOrThrow(place, TokenType.SEMICN);
            afterForStmt = new ForStmt(stream);
            rparentToken = stream.consumeOrThrow(place, TokenType.RPARENT);
            stmt = Stmt.parse(stream);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(forToken);
            ret.add(lparentToken);
            ret.add(initForStmt);
            ret.add(fisrtSemicnToken);
            ret.add(cond);
            ret.add(secondSemicnToken);
            ret.add(afterForStmt);
            ret.add(rparentToken);
            ret.add(stmt);
            return ret;
        }

        public ForStmt initForStmt() {
            return initForStmt;
        }

        public Cond cond() {
            return cond;
        }

        public ForStmt afterForStmt() {
            return afterForStmt;
        }

        public Stmt stmt() {
            return stmt;
        }
    }

    // Stmt → 'break' ';'
    public static class Stmt_Break implements StmtOption {
        private final Token breakToken;
        private final Token semicnToken;

        public Stmt_Break(TokenStream stream) {
            String place = "Stmt_Break()";
            breakToken = stream.consumeOrThrow(place, TokenType.BREAKTK);
            semicnToken = stream.consumeOrThrow(place, TokenType.SEMICN);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(breakToken);
            ret.add(semicnToken);
            return ret;
        }
    }

    // Stmt → 'continue' ';'
    public static class Stmt_Continue implements StmtOption {
        private final Token continueToken;
        private final Token semicnToken;

        public Stmt_Continue(TokenStream stream) {
            String place = "Stmt_Continue()";
            continueToken = stream.consumeOrThrow(place, TokenType.CONTINUETK);
            semicnToken = stream.consumeOrThrow(place, TokenType.SEMICN);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(continueToken);
            ret.add(semicnToken);
            return ret;
        }
    }

    // Stmt → 'return' [Exp] ';'
    public static class Stmt_Return implements StmtOption {
        private final Token returnToken;
        private final Exp exp;
        private final Token semicnToken;

        public Stmt_Return(TokenStream stream) {
            String place = "Stmt_Return()";
            returnToken = stream.consumeOrThrow(place, TokenType.RETURNTK);
            exp = new Exp(stream);
            semicnToken = stream.consumeOrThrow(place, TokenType.SEMICN);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(returnToken);
            ret.add(exp);
            ret.add(semicnToken);
            return ret;
        }

        public Exp exp() {
            return exp;
        }
    }

    // Stmt → LVal '=' 'getint' '(' ')' ';'
    public static class Stmt_LValGetint implements StmtOption {
        private final LVal lval;
        private final Token assignToken;
        private final Token getintToken;
        private final Token lparentToken;
        private final Token rparentToken;
        private final Token semicnToken;

        public Stmt_LValGetint(TokenStream stream) {
            String place = "Stmt_LValGetint()";
            lval = new LVal(stream);
            assignToken = stream.consumeOrThrow(place, TokenType.ASSIGN);
            getintToken = stream.consumeOrThrow(place, TokenType.GETINTTK);
            lparentToken = stream.consumeOrThrow(place, TokenType.LPARENT);
            rparentToken = stream.consumeOrThrow(place, TokenType.RPARENT);
            semicnToken = stream.consumeOrThrow(place, TokenType.SEMICN);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(lval);
            ret.add(assignToken);
            ret.add(getintToken);
            ret.add(lparentToken);
            ret.add(rparentToken);
            ret.add(semicnToken);
            return ret;
        }

        public LVal lval() {
            return lval;
        }
    }

    // Stmt → 'printf' '(' FormatString { ',' Exp } ')' ';'
    public static class Stmt_Printf implements StmtOption {
        private final Token printfToken;
        private final Token lparentToken;
        private final Token formatString;
        private final ArrayList<Token> commaTokens;
        private final ArrayList<Exp> exps;
        private final Token rparentToken;
        private final Token semicnToken;

        public Stmt_Printf(TokenStream stream) {
            String place = "Stmt_Printf()";
            printfToken = stream.consumeOrThrow(place, TokenType.PRINTFTK);
            lparentToken = stream.consumeOrThrow(place, TokenType.LPARENT);
            formatString = stream.consumeOrThrow(place, TokenType.STRCON);
            commaTokens = new ArrayList<>();
            exps = new ArrayList<>();
            while (stream.isNow(TokenType.COMMA)) {
                commaTokens.add(stream.consumeOrThrow(place, TokenType.COMMA));
                exps.add(new Exp(stream));
            }
            rparentToken = stream.consumeOrThrow(place, TokenType.RPARENT);
            semicnToken = stream.consumeOrThrow(place, TokenType.SEMICN);
        }

        @Override
        public ArrayList<Object> explore() {
            ArrayList<Object> ret = new ArrayList<>();
            ret.add(printfToken);
            ret.add(lparentToken);
            ret.add(formatString);
            for (int i = 0; i < exps.size(); i++) {
                ret.add(commaTokens.get(i));
                ret.add(exps.get(i));
            }
            ret.add(rparentToken);
            ret.add(semicnToken);
            return ret;
        }

        public Token formatString() {
            return formatString;
        }

        public ArrayList<Exp> exps() {
            return exps;
        }
    }
}
