package IR.value.instruction;

import IR.IRValue;
import IR.type.*;
import IR.value.BasicBlock;
import util.LLVMStrRegCounter;

import java.util.ArrayList;

public class GetElementPtrInst extends Instruction<PointerType> {
    // 在SysY中，该指令仅在访问数组（包括参数中退化为指针的数组）发挥作用，同时偏移值仅支持i32类型
    // 需要注意的是，该指令仅用于获取地址，不获取具体的值。

    private final IRType basisType;

    public GetElementPtrInst(IRValue<PointerType> pointerOperand, ArrayList<IRValue<IntegerType>> indexList, BasicBlock parent) {
        super(calcResultElementType(pointerOperand, indexList), parent);
        this.addOperand(pointerOperand);
        this.basisType = pointerOperand.type().referenceType();
        for (IRValue<IntegerType> index : indexList) {
            this.addOperand(index);
        }
    }

    private static PointerType calcResultElementType(IRValue<PointerType> pointerOperand, ArrayList<IRValue<IntegerType>> indexList) {
        if (indexList.isEmpty()) {
            throw new RuntimeException("When calcResultElementType(), the indexList is empty.");
        }
        // 第一层是pointerOperand指向的Type
        IRType resultElementType = pointerOperand.type().referenceType();
        // 随后逐层拆解
        for (int i = 1; i < indexList.size(); i++) {
            if (resultElementType instanceof PointerType referencedPointerType) {
                resultElementType = referencedPointerType.referenceType();
            } else if (resultElementType instanceof ArrayType referencedArrayType) {
                resultElementType = referencedArrayType.elementType();
            } else {
                throw new RuntimeException("When calcResultElementType(), the " + (i + 1) + " layer of pointerOperand " + pointerOperand +
                        " is not a PointerType or ArrayType, got " + resultElementType);
            }
        }
        // 得到GetElementPtr得到的指针类型
        return new PointerType(resultElementType);
    }

    @Override
    public String llvmStr(LLVMStrRegCounter counter) {
        StringBuilder sb = new StringBuilder();
        sb.append(counter.get(this));
        // 由于文法保证了数组访问不越界，故默认带上inbounds属性
        sb.append(" = getelementptr inbounds ");
        sb.append(this.basisType.llvmStr()).append(", ");
        sb.append(this.getOperand(0).type().llvmStr()).append(" ");
        sb.append(counter.get(this.getOperand(0)));
        for (int i = 1; i < this.getNumOperands(); i++) {
            sb.append(", ").append(this.getOperand(i).type().llvmStr()).append(" ");
            sb.append(counter.get(this.getOperand(i)));
        }
        return sb.toString();
    }
}
