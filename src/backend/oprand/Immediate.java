package backend.oprand;

public record Immediate(Integer value) implements TargetOperand, TargetAddress.ImmediateOffset {
    public static Immediate ZERO() {
        return new Immediate(0);
    }

    // 定位数组元素时使用
    public static Immediate TWO() {
        return new Immediate(2);
    }

    public static Immediate FF() {
        return new Immediate(0x7f);
    }

    // 计算常数偏移地址时使用
    public Immediate add(Immediate other) {
        return new Immediate(value + other.value);
    }

    // 定位数组元素时使用
    public Immediate multiplyFour() {
        return new Immediate(value * 4);
    }

    @Override
    public Immediate calc() {
        return this;
    }

    @Override
    public String mipsStr() {
        return "0x" + Integer.toHexString(value).toUpperCase();
    }
}
