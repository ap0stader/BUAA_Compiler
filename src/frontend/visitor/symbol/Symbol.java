package frontend.visitor.symbol;

import IR.IRValue;
import IR.type.IRType;
import frontend.lexer.Token;
import global.Config;


public abstract class Symbol {
    private final IRType type;
    private final String name;
    private final int line;
    private IRValue irValue;

    protected Symbol(IRType type, Token ident) {
        this.type = type;
        this.name = ident.strVal();
        this.line = ident.line();
    }

    public String typeDisplayStr() {
        return this.type.displayStr();
    }

    public int line() {
        return this.line;
    }

    public String name() {
        return this.name;
    }

    public IRValue irValue() {
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

    public void setIRValue(IRValue irValue) {
        if (this.irValue != null) {
            this.irValue = irValue;
        } else {
            if (Config.visitorThrowable) {
                throw new RuntimeException("The value of VarSymbol '" + this.name +
                        "' at line " + this.line + " has set.");
            }
        }
    }
}
