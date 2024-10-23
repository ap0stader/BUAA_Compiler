package IR.type;

public record VoidType() implements IRType {
    @Override
    public String displayStr() {
        return "Void";
    }

    @Override
    public String llvmStr() {
        return "void";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VoidType;
    }
}
