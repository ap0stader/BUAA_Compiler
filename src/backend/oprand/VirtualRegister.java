package backend.oprand;

public final class VirtualRegister implements TargetRegister, Comparable<VirtualRegister> {
    private final int number;

    private VirtualRegister(int number) {
        this.number = number;
    }

    @Override
    public int compareTo(VirtualRegister o) {
        return Integer.compare(this.number, o.number);
    }

    @Override
    public String mipsStr() {
        return "&vr" + this.number;
    }

    @Override
    public String toString() {
        return this.mipsStr();
    }

    private static int counter = 0;

    public static VirtualRegister create() {
        return new VirtualRegister(counter++);
    }
}
