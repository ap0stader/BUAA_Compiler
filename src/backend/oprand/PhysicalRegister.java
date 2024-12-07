package backend.oprand;

import java.util.HashMap;

public class PhysicalRegister extends TargetRegister {
    public enum Reg {
        // 固定为0，保留寄存器
        ZERO(0, "zero"),
        // 汇编器保留，不可被使用
        AT(1, "at"),
        // 返回值
        V0(2, "v0"),
        // 参数
        A0(4, "a0"),
        A1(5, "a1"),
        A2(6, "a2"),
        A3(7, "a3"),
        // 全局指针，用于访问全局变量，保留寄存器
        GP(28, "gp"),
        // 栈指针，用于操作栈，保留寄存器
        SP(29, "sp"),
        // 返回地址
        RA(31, "ra"),
        // 临时变量寄存器（11个）
        T0(3),
        T1(8),
        T2(9),
        T3(10),
        T4(11),
        T5(12),
        T6(13),
        T7(14),
        T8(15),
        T9(24),
        T10(25),
        // 保存变量寄存器（11个）
        S0(16),
        S1(17),
        S2(18),
        S3(19),
        S4(20),
        S5(21),
        S6(22),
        S7(23),
        S8(26),
        S9(27),
        S10(30);

        private final String name;

        Reg(int number) {
            this.name = String.valueOf(number);
        }

        Reg(int number, String name) {
            this.name = name;
        }
    }

    private final Reg reg;

    private PhysicalRegister(Reg reg) {
        this.reg = reg;
    }

    @Override
    public String mipsStr() {
        return "$" + this.reg.name;
    }

    private static final HashMap<Reg, PhysicalRegister> physicalRegisters = new HashMap<>();

    static {
        for (Reg reg : Reg.values()) {
            physicalRegisters.put(reg, new PhysicalRegister(reg));
        }
    }

    public static PhysicalRegister getPhysicalRegister(Reg reg) {
        if (reg == Reg.AT) {
            throw new RuntimeException("When getPhysicalRegister(), try to get $at, which is illegal");
        }
        if (physicalRegisters.containsKey(reg)) {
            return physicalRegisters.get(reg);
        } else {
            throw new RuntimeException("When getPhysicalRegister(), try to get register which is undefined");
        }
    }
}
