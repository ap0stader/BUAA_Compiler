package frontend.visitor.symbol;

import IR.type.Type;
import IR.value.Value;
import global.Config;

public abstract class Symbol {
    private final Type type;
    private final String name;
    private final int line;
    private Value value;

    protected Symbol(Type type, String name, int line) {
        this.type = type;
        this.name = name;
        this.line = line;
        this.value = null;
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

    public Value value() {
        if (this.value != null) {
            return this.value;
        } else {
            if (Config.visitorThrowable) {
                throw new RuntimeException("The value of VarSymbol '" + this.name +
                        "' at line " + this.line + " is null.");
            } else {
                return null;
            }
        }
    }

    public void setValue(Value value) {
        if (this.value != null) {
            this.value = value;
        } else {
            if (Config.visitorThrowable) {
                throw new RuntimeException("The value of VarSymbol '" + this.name +
                        "' at line " + this.line + " has set.");
            }
        }
    }
}
