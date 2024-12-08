package backend.oprand;

public final class VirtualRegister implements TargetRegister {
    private final int number;

    private VirtualRegister(int number) {
        this.number = number;
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

    public static VirtualRegister getVirtualRegister() {
        return new VirtualRegister(counter++);
    }
}
