package backend.oprand;

public class VirtualRegister extends TargetRegister {
    private final int number;

    private VirtualRegister(int number) {
        this.number = number;
    }

    @Override
    public String mipsStr() {
        return "&vr" + this.number;
    }

    private static int counter = 0;

    public static VirtualRegister getVirtualRegister() {
        return new VirtualRegister(counter++);
    }
}
