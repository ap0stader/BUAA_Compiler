package backend.oprand;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class TargetAddress<B, C extends TargetAddress<B, ?>> implements TargetOperand {
    // 立即数偏移
    public interface ImmediateOffset {
        Immediate calc();
    }

    // 基地址
    protected final B base;
    // 立即数偏移列表
    protected final ArrayList<ImmediateOffset> immediateOffsetList;

    // 初始化对象时使用
    protected TargetAddress(B base, ImmediateOffset... immediateOffsets) {
        this.base = base;
        this.immediateOffsetList = new ArrayList<>(Arrays.asList(immediateOffsets));
    }

    // 克隆对象时使用
    protected TargetAddress(TargetAddress<B, C> oldAddress, ImmediateOffset... newImmediateOffsets) {
        this.base = oldAddress.base;
        this.immediateOffsetList = new ArrayList<>(oldAddress.immediateOffsetList);
        this.immediateOffsetList.addAll(Arrays.asList(newImmediateOffsets));
    }

    // 计算立即数偏移
    protected Immediate immediateOffset() {
        return this.immediateOffsetList.stream().map(ImmediateOffset::calc).reduce(Immediate.ZERO, Immediate::add);
    }

    public abstract C addImmediateOffset(ImmediateOffset immediateOffset);

    // 使用Label作为基地址的是全局变量，一个Label对应了一个对象
    // 对于非数组对象类型的访问，不需要任何偏移
    // 对于数组对象类型的访问，要么是单个常数的偏移，要么是一个寄存器的结果
    public static final class LabelBase extends TargetAddress<Label, LabelBase> {
        private final TargetRegister registerOffset;

        public LabelBase(Label label) {
            super(label);
            this.registerOffset = null;
        }

        private LabelBase(LabelBase oldAddress, TargetRegister registerOffset) {
            super(oldAddress);
            this.registerOffset = registerOffset;
        }

        private LabelBase(LabelBase oldAddress, ImmediateOffset immediateOffset) {
            super(oldAddress, immediateOffset);
            this.registerOffset = oldAddress.registerOffset;
        }

        public LabelBase addRegisterOffset(TargetRegister registerOffset) {
            if (this.registerOffset == null && this.immediateOffsetList.isEmpty()) {
                // 没有寄存器偏移和立即数偏移
                return new LabelBase(this, registerOffset);
            } else {
                throw new RuntimeException("When setRegisterOffset(), not both of registerOffset immediateOffset are null");
            }
        }

        @Override
        public LabelBase addImmediateOffset(ImmediateOffset immediateOffset) {
            if (registerOffset == null) {
                // 没有寄存器偏移
                return new LabelBase(this, immediateOffset);
            } else {
                throw new RuntimeException("When setImmediateOffset(), registerOffset is not null");
            }
        }

        @Override
        public String mipsStr() {
            if (registerOffset != null) {
                return this.base.mipsStr() + "+" + registerOffset.mipsStr();
            } else if (!immediateOffsetList.isEmpty()) {
                return this.base.mipsStr() + "+" + this.immediateOffset().mipsStr();
            } else {
                return this.base.mipsStr();
            }
        }

        @Override
        public String toString() {
            return "LabelBase{" +
                    "base=" + base +
                    ", immediateOffsetList=" + immediateOffsetList +
                    ", registerOffset=" + registerOffset +
                    '}';
        }
    }


    // 使用寄存器作为基地址的是进步
    public static final class RegisterBase extends TargetAddress<PhysicalRegister, RegisterBase> {
        public RegisterBase(PhysicalRegister base, ImmediateOffset immediateOffset) {
            super(base, immediateOffset);
        }

        private RegisterBase(RegisterBase oldAddress, ImmediateOffset immediateOffset) {
            super(oldAddress, immediateOffset);
        }

        @Override
        public RegisterBase addImmediateOffset(ImmediateOffset immediateOffset) {
            return new RegisterBase(this, immediateOffset);
        }

        @Override
        public String mipsStr() {
            return this.immediateOffset().mipsStr() + "(" + this.base.mipsStr() + ")";
        }

        @Override
        public String toString() {
            return "RegisterBase{" +
                    "base=" + base +
                    ", immediateOffsetList=" + immediateOffsetList +
                    '}';
        }
    }
}
