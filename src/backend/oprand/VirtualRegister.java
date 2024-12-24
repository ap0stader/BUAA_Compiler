package backend.oprand;

public final class VirtualRegister implements TargetRegister, Comparable<VirtualRegister> {
    private static int counter = 0;

    private final int number;
    private TargetAddress<?, ?> address = null;

    public VirtualRegister() {
        this.number = counter++;
    }

    public void setAddress(TargetAddress<?, ?> address) {
        if (this.address == null) {
            this.address = address;
        } else {
            throw new RuntimeException("When setAddress(), address has already been set");
        }
    }

    public TargetAddress<?, ?> address() {
        // WARNING address不应包含其他的VirtualRegister否则会导致替换失败
        return address;
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
