package backend.oprand;

public enum PhysicalRegister implements TargetRegister {
    // 固定为0，保留寄存器
    ZERO(0, "zero"),
    // 栈指针，用于操作栈，保留寄存器
    SP(29, "sp"),
    // 汇编器保留，不可被使用
    // AT(1, "at"),
    // 返回值和系统调用号，保留寄存器
    V0(2, "v0"),
    // 参数寄存器（4个）
    A0(4, "a0"),
    A1(5, "a1"),
    A2(6, "a2"),
    A3(7, "a3"),
    // 临时变量寄存器（11个）
    T0(3), // $v1
    T1(8), // $t0
    T2(9), // $t1
    T3(10), // $t2
    T4(11), // $t3
    T5(12), // $t4
    T6(13), // $t5
    T7(14), // $t6
    T8(15), // $t7
    T9(24), // $t8
    T10(25), // $t9
    // 保存变量寄存器（12个）
    S0(16), // $s0
    S1(17), // $s1
    S2(18), // $s2
    S3(19), // $s3
    S4(20), // $s4
    S5(21), // $s5
    S6(22), // $s6
    S7(23), // $s7
    S8(26), // $k0
    S9(27), // $k1
    S10(28), // $gp，初始值虽然不是0，但是并不会假设寄存器的初始值为0
    S11(30), // $fp
    // 返回地址，在保存之后可以被分配为保存变量寄存器
    RA(31, "ra");

    private final String name;

    PhysicalRegister(int number) {
        this.name = String.valueOf(number);
    }

    @SuppressWarnings("unused")
    PhysicalRegister(int number, String name) {
        this.name = name;
    }

    @Override
    public String mipsStr() {
        return "$" + this.name;
    }

    // 参数寄存器
    public static boolean isArgumentRegister(PhysicalRegister register) {
        return register == A0 || register == A1 || register == A2 || register == A3;
    }

    public static int argumentNumberOfArgumentRegister(PhysicalRegister register) {
        return switch (register) {
            case A0 -> 0;
            case A1 -> 1;
            case A2 -> 2;
            case A3 -> 3;
            default ->
                    throw new IllegalArgumentException("When argumentNumberOfArgumentRegister(), the register is not argument register");
        };
    }

    public static PhysicalRegister argumentRegisterOfArgumentNumber(int number) {
        return switch (number) {
            case 0 -> A0;
            case 1 -> A1;
            case 2 -> A2;
            case 3 -> A3;
            default -> null;
        };
    }
}
