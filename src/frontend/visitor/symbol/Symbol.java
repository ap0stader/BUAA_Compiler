package frontend.visitor.symbol;

import IR.IRValue;
import IR.type.IRType;
import frontend.lexer.Token;
import global.Config;

public abstract class Symbol<T extends IRType, VT extends IRValue<?>> {
    private final T type;
    private final String name;
    private final int line;
    private VT irValue = null;

    protected Symbol(T type, Token ident) {
        this.type = type;
        this.name = ident.strVal();
        this.line = ident.line();
    }

    public T type() {
        return type;
    }

    public String name() {
        return name;
    }

    public int line() {
        return line;
    }

    public VT irValue() {
        if (this.irValue != null) {
            return this.irValue;
        } else {
            if (Config.visitorThrowable) {
                throw new RuntimeException("The value of VarSymbol '" + this.name +
                        "' at line " + this.line + " is null.");
            } else {
                return null;
            }
        }
    }

    public void setIRValue(VT irValue) {
        this.irValue = irValue;
    }
}
