package frontend.visitor;

import IR.type.*;
import frontend.lexer.Token;
import frontend.type.TokenType;
import frontend.visitor.symbol.SymbolType;
import global.Config;

import java.util.ArrayList;

class Translator {
    private Translator() {
    }

    static ArrayList<Integer> translateStringConst(Token stringConst) {
        try {
            assert stringConst.type() == TokenType.STRCON;
            String stringConstString = stringConst.strVal();
            ArrayList<Integer> ret = new ArrayList<>();
            // 跳过两侧的双引号
            for (int i = 1; i < stringConstString.length() - 1; i++) {
                if (stringConstString.charAt(i) == '\\') {
                    // 转义字符
                    if (i + 1 >= stringConstString.length() - 1) {
                        throw new RuntimeException("When translateCharConst(), the escape character at the end of the string is uncompleted.");
                    } else {
                        // 跳过转义符
                        i++;
                    }
                    ret.add(escapeCharToInt(stringConstString.charAt(i)));
                } else {
                    // 普通字符
                    ret.add(charToInt(stringConstString.charAt(i)));
                }
            }
            // 主动补充结尾的\0
            ret.add(0);
            return ret;
        } catch (Exception e) {
            if (Config.visitorThrowable) {
                throw new RuntimeException("When translateStringConst(), caught exception " + e.getMessage()
                        + ". The StringConst is " + stringConst);
            } else {
                return new ArrayList<>();
            }
        }
    }

    static Integer translateCharConst(Token charConst) {
        // 由于char参加运算的方式为先零拓展为int再参加运算，故此处包括子函数均直接提升为int
        try {
            assert charConst.type() == TokenType.CHRCON;
            String charConstString = charConst.strVal();
            // 跳过两侧的单引号
            if (charConstString.charAt(1) == '\\') {
                // 转义字符
                assert charConstString.length() == 4;
                return escapeCharToInt(charConstString.charAt(2));
            } else {
                // 普通字符
                assert charConstString.length() == 3;
                return charToInt(charConstString.charAt(1));
            }
        } catch (Exception e) {
            if (Config.visitorThrowable) {
                throw new RuntimeException("When translateCharConst(), caught exception " + e.getMessage()
                        + ". The CharConst is " + charConst);
            } else {
                return 0;
            }
        }
    }

    private static int charToInt(char character) {
        if (32 <= character && character <= 126 && character != '\\') {
            return character;
        } else {
            if (Config.visitorThrowable) {
                throw new RuntimeException("When charToInt, got unexpected character '" + character
                        + "'(ASCII:" + (int) character + ")");
            } else {
                return 0;
            }
        }
    }

    private static int escapeCharToInt(char escapeCharacter) {
        return switch (escapeCharacter) {
            case 'a' -> 7;
            case 'b' -> 8;
            case 't' -> 9;
            case 'n' -> 10;
            case 'v' -> 11;
            case 'f' -> 12;
            case '"' -> 34;
            case '\'' -> 39;
            case '\\' -> 92;
            case '0' -> 0;
            default -> {
                if (Config.visitorThrowable) {
                    throw new RuntimeException("When escapeCharToInt, got unexpected escape character '\\"
                            + escapeCharacter + "'");
                } else {
                    yield 0;
                }
            }
        };
    }

    static SymbolType.Const getConstIRType(Token bType, Integer length) {
        if (bType.type() == TokenType.INTTK || bType.type() == TokenType.CHARTK) {
            if (length == null) {
                // 数组长度可以为0，null表示没有长度
                if (bType.type() == TokenType.CHARTK) {
                    return IRType.getInt8Ty();
                } else { // bType.type() == TokenType.INTTK
                    return IRType.getInt32Ty();
                }
            } else if (length >= 0) {
                if (bType.type() == TokenType.CHARTK) {
                    return new ArrayType(IRType.getInt8Ty(), length);
                } else { // bType.type() == TokenType.INTTK
                    return new ArrayType(IRType.getInt32Ty(), length);
                }
            } else {
                throw new RuntimeException("When getConstIRType(), the numElements " + length + " of " + bType + " is illegal");
            }
        } else {
            throw new RuntimeException("When getConstIRType(), got unexpected bType " + bType);
        }
    }


    static SymbolType.Var getVarIRType(Token bType, Integer length) {
        if (bType.type() == TokenType.INTTK || bType.type() == TokenType.CHARTK) {
            if (length == null) {
                // 数组长度可以为0，null表示没有长度
                if (bType.type() == TokenType.CHARTK) {
                    return IRType.getInt8Ty();
                } else { // bType.type() == TokenType.INTTK
                    return IRType.getInt32Ty();
                }
            } else if (length >= 0) {
                if (bType.type() == TokenType.CHARTK) {
                    return new ArrayType(IRType.getInt8Ty(), length);
                } else { // bType.type() == TokenType.INTTK
                    return new ArrayType(IRType.getInt32Ty(), length);
                }
            } else {
                throw new RuntimeException("When getVarIRType(), the numElements " + length + " of " + bType + " is illegal");
            }
        } else {
            throw new RuntimeException("When getVarIRType(), got unexpected bType " + bType);
        }
    }

    static SymbolType.Arg getArgIRType(Token bType, boolean arrayDecay) {
        if (bType.type() == TokenType.INTTK || bType.type() == TokenType.CHARTK) {
            if (arrayDecay) {
                if (bType.type() == TokenType.CHARTK) {
                    return new PointerType(IRType.getInt8Ty(), true);
                } else { // bType.type() == TokenType.INTTK
                    return new PointerType(IRType.getInt32Ty(), true);
                }
            } else {
                if (bType.type() == TokenType.CHARTK) {
                    return IRType.getInt8Ty();
                } else { // bType.type() == TokenType.INTTK
                    return IRType.getInt32Ty();
                }
            }
        } else {
            throw new RuntimeException("When getArgIRType(), got unexpected bType " + bType);
        }
    }

    static FunctionType getFuncIRType(Token funcType, ArrayList<SymbolType.Arg> parameters) {
        IRType returnType;
        if (funcType.type() == TokenType.INTTK) {
            returnType = IRType.getInt32Ty();
        } else if (funcType.type() == TokenType.CHARTK) {
            returnType = IRType.getInt8Ty();
        } else if (funcType.type() == TokenType.VOIDTK) {
            returnType = IRType.getVoidTy();
        } else {
            throw new RuntimeException("When getFuncIRType(), got unexpected funcType " + funcType);
        }
        return new FunctionType(returnType, new ArrayList<>(parameters));
    }
}
