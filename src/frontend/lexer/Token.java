package frontend.lexer;

import frontend.type.TokenType;

public record Token(TokenType type, String strVal, int line) {
}
