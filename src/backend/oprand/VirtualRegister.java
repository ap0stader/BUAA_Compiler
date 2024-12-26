package backend.oprand;

public final class VirtualRegister implements TargetRegister, Comparable<VirtualRegister> {
    private static int counter = 0;

    private final int number;
    private TargetAddress<?, ?> address = null;
    private final boolean isPreDefined;
    private final PhysicalRegister preAllocation;

    public VirtualRegister() {
        this.number = ++counter;
        this.isPreDefined = false;
        this.preAllocation = null;
    }

    public VirtualRegister(boolean isPreDefined) {
        this.number = counter++;
        this.isPreDefined = isPreDefined;
        this.preAllocation = null;
    }

    public VirtualRegister(PhysicalRegister preAllocation) {
        this.number = ++counter;
        this.isPreDefined = true;
        this.preAllocation = preAllocation;
    }

    public void setAddress(TargetAddress<?, ?> address) {
        if (this.address == null && this.preAllocation == null) {
            this.address = address;
        } else {
            throw new RuntimeException("When setAddress(), address has already been set or the VirtualRegister is preAllocated.");
        }
    }

    public TargetAddress<?, ?> address() {
        // WARNING address不应包含其他的VirtualRegister，否则会导致替换失败
        return address;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isPreDefined() {
        return isPreDefined;
    }

    public boolean isPreAllocated() {
        return preAllocation != null;
    }

    public PhysicalRegister preAllocation() {
        return preAllocation;
    }

    @Override
    public int compareTo(VirtualRegister o) {
        return Integer.compare(this.number, o.number);
    }

    @Override
    public String mipsStr() {
        if (this.isPreAllocated()) {
            return this.preAllocation.mipsStr();
        } else {
            return "&vr" + this.number;
        }
    }

    @Override
    public String toString() {
        return this.mipsStr();
    }
}
