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
}
