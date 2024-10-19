package frontend.visitor;

import IR.type.ArrayType;
import IR.type.IRType;
import IR.type.IntegerType;
import IR.type.PointerType;
import frontend.lexer.Token;
import frontend.type.TokenType;
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
                    }
                    ret.add(escapeCharToInt(stringConstString.charAt(i + 1)));
                } else {
                    // 普通字符
                    ret.add(charToInt(stringConstString.charAt(i)));
                }
            }
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
                throw new RuntimeException("When charToInt(), got unexpected character '" + character
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
                    throw new RuntimeException("When escapeCharToInt(), got unexpected escape character '\\"
                            + escapeCharacter + "'");
                } else {
                    yield 0;
                }
            }
        };
    }

    static IRType.ConstSymbolType translateConstType(Token bType, int length) {
        if (bType.type() == TokenType.INTTK || bType.type() == TokenType.CHARTK) {
            if (length == 0) {
                if (bType.type() == TokenType.CHARTK) {
                    return new IntegerType.Char();
                } else { // bType.type() == TokenType.INTTK
                    return new IntegerType.Int();
                }
            } else if (length > 0) {
                if (bType.type() == TokenType.CHARTK) {
                    return new ArrayType(new IntegerType.Char(), length);
                } else { // bType.type() == TokenType.INTTK
                    return new ArrayType(new IntegerType.Int(), length);
                }
            } else {
                throw new RuntimeException("When translateConstType(), the length " + length + " of " + bType + " is illegal");
            }
        } else {
            throw new RuntimeException("When translateConstType(), got unexpected bType " + bType);
        }
    }

    public static IRType.VarSymbolType translateVarType(Token bType, int length, boolean arrayDecay) {
        if (bType.type() == TokenType.INTTK || bType.type() == TokenType.CHARTK) {
            if (arrayDecay) {
                if (bType.type() == TokenType.CHARTK) {
                    return new PointerType(new IntegerType.Char(), true);
                } else { // bType.type() == TokenType.INTTK
                    return new PointerType(new IntegerType.Int(), true);
                }
            } else {
                if (length == 0) {
                    if (bType.type() == TokenType.CHARTK) {
                        return new IntegerType.Char();
                    } else { // bType.type() == TokenType.INTTK
                        return new IntegerType.Int();
                    }
                } else if (length > 0) {
                    if (bType.type() == TokenType.CHARTK) {
                        return new ArrayType(new IntegerType.Char(), length);
                    } else { // bType.type() == TokenType.INTTK
                        return new ArrayType(new IntegerType.Int(), length);
                    }
                } else {
                    throw new RuntimeException("When translateVarType(), the length " + length + " of " + bType + " is illegal");
                }
            }
        } else {
            throw new RuntimeException("When translateVarType(), got unexpected bType " + bType);
        }
    }
}
