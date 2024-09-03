package frontend.type;

public abstract class ASTNodeWithOption<T extends ASTNodeOption> {
    private final T option;

    protected ASTNodeWithOption(T option) {
        this.option = option;
    }

    public T extract() {
        return this.option;
    }
}
