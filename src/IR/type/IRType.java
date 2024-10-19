package IR.type;

public interface IRType {
    String displayStr();

    interface VarSymbolType extends IRType {}

    interface ConstSymbolType extends IRType {}

    interface FuncSymbolType extends IRType {}
}
