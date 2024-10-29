package IR;

public record IRUse(
        IRUser<?> user,
        IRValue<?> value
) {
}
