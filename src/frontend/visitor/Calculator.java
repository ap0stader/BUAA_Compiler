package frontend.visitor;

import IR.type.IRType;
import frontend.lexer.Token;
import frontend.parser.expression.*;
import frontend.type.TokenType;
import frontend.visitor.symbol.ConstSymbol;
import frontend.visitor.symbol.Symbol;

class Calculator {
    private final SymbolTable symbolTable;

    Calculator(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    private static class UseVariableContent extends RuntimeException {
        private UseVariableContent(String place, Token ident) {
            super("When " + place + ", used variable content, which identifier token is " + ident);
        }
    }

    Integer calculateConstExp(ConstExp constExp) {
        return this.calculateAddExp(constExp.addExp());
    }

    Integer calculateExp(Exp exp) {
        return this.calculateAddExp(exp.addExp());
    }

    Integer calculateExpOrNull(Exp exp) {
        try {
            return this.calculateAddExp(exp.addExp());
        } catch (UseVariableContent e) {
            return null;
        }
    }

    private Integer calculateAddExp(AddExp addExp) {
        Integer result = this.calculateMulExp(addExp.mulExps().get(0));
        for (int i = 0; i < addExp.symbols().size(); i++) {
            if (addExp.symbols().get(i).type() == TokenType.PLUS) {
                result += this.calculateMulExp(addExp.mulExps().get(i + 1));
            } else if (addExp.symbols().get(i).type() == TokenType.MINU) {
                result -= this.calculateMulExp(addExp.mulExps().get(i + 1));
            } else {
                throw new RuntimeException("When calculateAddExp(), got unexpected symbol " + addExp.symbols().get(i)
                        + ", expected " + TokenType.PLUS + "/" + TokenType.MINU);
            }
        }
        return result;
    }

    private Integer calculateMulExp(MulExp mulExp) {
        Integer result = this.calculateUnaryExp(mulExp.unaryExps().get(0));
        for (int i = 0; i < mulExp.symbols().size(); i++) {
            if (mulExp.symbols().get(i).type() == TokenType.MULT) {
                result *= this.calculateUnaryExp(mulExp.unaryExps().get(i + 1));
            } else if (mulExp.symbols().get(i).type() == TokenType.DIV) {
                // 发现了除以0，强制置为0
                if (this.calculateUnaryExp(mulExp.unaryExps().get(i + 1)) == 0) {
                    result = 0;
                    System.out.println("When calculateMulExp(), caught a division of error, forced the result to 0!");
                } else {
                    result /= this.calculateUnaryExp(mulExp.unaryExps().get(i + 1));
                }
            } else if (mulExp.symbols().get(i).type() == TokenType.MOD) {
                result %= this.calculateUnaryExp(mulExp.unaryExps().get(i + 1));
            } else {
                throw new RuntimeException("When calculateMulExp(), got unexpected symbol " + mulExp.symbols().get(i)
                        + ", expected " + TokenType.MULT + "/" + TokenType.DIV + "/" + TokenType.MOD);
            }
        }
        return result;
    }

    private Integer calculateUnaryExp(UnaryExp unaryExp) {
        UnaryExp.UnaryExpOption unaryExpExtract = unaryExp.extract();
        if (unaryExpExtract instanceof UnaryExp.UnaryExp_UnaryOp unaryExp_unaryOp) {
            if (unaryExp_unaryOp.unaryOp().symbol().type() == TokenType.PLUS) {
                return this.calculateUnaryExp(unaryExp_unaryOp.unaryExp());
            } else if (unaryExp_unaryOp.unaryOp().symbol().type() == TokenType.MINU) {
                return -this.calculateUnaryExp(unaryExp_unaryOp.unaryExp());
            } else {
                throw new RuntimeException("When calculateUnaryExp(), got unexpected symbol " + unaryExp_unaryOp.unaryOp().symbol()
                        + ", expected " + TokenType.PLUS + "/" + TokenType.MINU);
            }
        } else if (unaryExpExtract instanceof UnaryExp.UnaryExp_PrimaryExp unaryExp_primaryExp) {
            return this.calculatePrimaryExp(unaryExp_primaryExp.primaryExp());
        } else if (unaryExpExtract instanceof UnaryExp.UnaryExp_IdentFuncCall unaryExp_identFuncCall) {
            throw new UseVariableContent("calculateUnaryExp()", unaryExp_identFuncCall.ident());
        } else {
            throw new RuntimeException("When calculateUnaryExp(), got unknown type of UnaryExp ("
                    + unaryExpExtract.getClass().getSimpleName() + ")");
        }
    }

    private Integer calculatePrimaryExp(PrimaryExp primaryExp) {
        PrimaryExp.PrimaryExpOption primaryExpExtract = primaryExp.extract();
        if (primaryExpExtract instanceof PrimaryExp.PrimaryExp_Exp primaryExp_exp) {
            return this.calculateExp(primaryExp_exp.exp());
        } else if (primaryExpExtract instanceof PrimaryExp.PrimaryExp_LVal primaryExp_lVal) {
            return this.calculateLVal(primaryExp_lVal.lVal());
        } else if (primaryExpExtract instanceof PrimaryExp.PrimaryExp_Number primaryExp_number) {
            return Integer.parseInt(primaryExp_number.number().intConst().strVal());
        } else if (primaryExpExtract instanceof PrimaryExp.PrimaryExp_Character primaryExp_character) {
            // 由于char参加运算的方式为先零拓展为int再参加运算，故此处可以直接提升为int
            return Translator.translateCharConst(primaryExp_character.character().charConst());
        } else {
            throw new RuntimeException("When calculatePrimaryExp(), got unknown type of PrimaryExp ("
                    + primaryExpExtract.getClass().getSimpleName() + ")");
        }
    }

    private Integer calculateLVal(LVal lVal) {
        Symbol<?, ?> symbol = this.symbolTable.searchOrError(lVal.ident());
        if (symbol != null) {
            if (symbol instanceof ConstSymbol constSymbol) {
                if (lVal.getType() == LVal.Type.BASIC) {
                    return constSymbol.getInitValAtIndex(lVal.ident(), 0);
                } else if (lVal.getType() == LVal.Type.ARRAY) {
                    return constSymbol.getInitValAtIndex(lVal.ident(), this.calculateExp(lVal.exp()));
                } else {
                    throw new RuntimeException("When calculateLVal(), got unknown type of LVal (" + lVal.getType() + ")");
                }
            } else {
                throw new UseVariableContent("calculateLVal()", lVal.ident());
            }
        } else {
            return 0;
        }
    }
}
