package frontend.visitor.symbol;

import IR.IRValue;
import IR.type.IRType;
import global.Config;

public abstract class Symbol {
    private final IRType type;
    private final String name;
    private final int line;
    private IRValue irValue;

    protected Symbol(IRType type, String name, int line) {
        this.type = type;
        this.name = name;
        this.line = line;
        this.irValue = null;
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
