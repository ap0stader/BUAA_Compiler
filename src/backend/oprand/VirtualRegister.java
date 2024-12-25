package backend.oprand;

public final class VirtualRegister implements TargetRegister, Comparable<VirtualRegister> {
    private static int counter = 0;

    private final int number;
    private TargetAddress<?, ?> address = null;
    private final boolean preDefined;

    public VirtualRegister(boolean preDefined) {
        this.number = counter++;
        this.preDefined = preDefined;
    }

    public void setAddress(TargetAddress<?, ?> address) {
        if (this.address == null) {
            this.address = address;
        } else {
            throw new RuntimeException("When setAddress(), address has already been set");
        }
    }

    public TargetAddress<?, ?> address() {
        // WARNING address不应包含其他的VirtualRegister，否则会导致替换失败
        return address;
    }

    public boolean preDefined() {
        return preDefined;
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
}
