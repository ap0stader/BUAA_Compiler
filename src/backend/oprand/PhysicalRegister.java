package backend.oprand;

public enum PhysicalRegister implements TargetRegister {
    // 固定为0，保留寄存器
    ZERO(0, "zero"),
    // 栈指针，用于操作栈，保留寄存器
    SP(29, "sp"),
    // 汇编器保留，不可被使用
    // AT(1, "at"),
    // 返回值和系统调用号，调度寄存器
    // 调度寄存器的值的有效周期局限于一条指令，无需在函数调用过程中进行保存
    // $v0用作调度寄存器的合理性在于
    // 1.作为返回值寄存器写入时，是在函数尾声前的最后执行的一条Move，这条Move只有Use，即便该Use的VirtualRegister分配为$v0，只会产生lw $v0 -> move $v0, $v0
    // 2.作为返回值寄存器读取时，是在函数调用结束后立即执行的一条Move，这条Move只有Def，即便该Def的VirtualRegister分配为$v0，只会产生move $v0, $v0 -> sw $v0
    // 3.作为系统调用号使用时，Syscall指令没有Use也没有Def，不会产生冲突
    V0(2, "v0"),
    // 调度寄存器
    V1(3, "v1"),
    // 调度寄存器只需要两个，因为Use最多的Binary也只会Use两个VirtualRegister
    // 参数寄存器（4个）
    A0(4, "a0"),
    A1(5, "a1"),
    A2(6, "a2"),
    A3(7, "a3"),
    // 临时变量寄存器（10个）
    T0(8, "t0"),
    T1(9, "t1"),
    T2(10, "t2"),
    T3(11, "t3"),
    T4(12, "t4"),
    T5(13, "t5"),
    T6(14, "t6"),
    T7(15, "t7"),
    T8(24, "t8"),
    T9(25, "t9"),
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
    // 返回地址，在保存之后可以被分配为临时变量寄存器
    RA(31, "ra");

    public static final int DISPATCH_REGISTER_SIZE = 2;
    public static final int TEMP_REGISTER_SIZE = 10;
    public static final int SAVED_REGISTER_SIZE = 12;

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
