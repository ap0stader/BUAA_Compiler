package global.error;

public enum ErrorType {
    ILLEGAL_CHAR("a"),
    DUPLICATED_IDENT("b"),
    UNDEFINED_IDENT("c"),
    FUNCRPARAMS_NUM_MISMATCH("d"),
    FUNCRPARAM_TYPE_MISMATCH("e"),
    RETURN_TYPE_MISMATCH("f"),
    MISSING_RETURN("g"),
    TRY_MODIFY_CONST("h"),
    MISSING_SEMICN("i"),
    MISSING_RPARENT("j"),
    MISSING_RBRACK("k"),
    PRINTF_RPARAMS_NUM_MISMATCH("l"),
    BREAK_CONTINUE_OUTSIDE_LOOP("m");

    private final String code;

    ErrorType(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return this.code;
    }
}
