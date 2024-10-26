package IR.value.instruction;

import IR.IRValue;
import IR.type.ArrayType;
import IR.type.PointerType;
import IR.type.StructType;
import IR.value.BasicBlock;
import util.LLVMStrRegCounter;

import java.util.ArrayList;

public class GetElementPtrInst extends Instruction {
    // getelementptr指令非常灵活，可以获得数组、结构体等聚合类型的指针
    // 但是在Sysy中，该指令仅在访问数组（包括参数中退化为指针的数组）发挥作用，同时偏移值仅支持i32类型
    // 需要注意的是，该指令仅用于获取地址，不获取具体的值。作为Value，其type为PointerType
    // 由于文法保证了数组访问不越界，故默认带上inbounds属性
    private final ArrayList<Integer> indexList;

    public GetElementPtrInst(IRValue pointerOperand, ArrayList<Integer> indexList, BasicBlock parent) {
        super(calcResultElementType(pointerOperand, indexList), parent);
        this.indexList = indexList;
        this.addOperand(pointerOperand);
    }

    private static PointerType calcResultElementType(IRValue pointerOperand, ArrayList<Integer> indexList) {
        if (indexList.isEmpty()) {
            throw new RuntimeException("When calcResultElementType(), the indexList is empty.");
        }
        if (pointerOperand.type() instanceof PointerType sourceElementType) {
            PointerType resultElementType = sourceElementType;
            for (int i = 1; i < indexList.size(); i++) {
                if (resultElementType.referenceType() instanceof PointerType referencedPointerType) {
                    resultElementType = referencedPointerType;
                } else if (resultElementType.referenceType() instanceof ArrayType referencedArrayType) {
                    resultElementType = new PointerType(referencedArrayType.elementType(), false);
                } else if (resultElementType.referenceType() instanceof StructType referencedStructType) {
                    resultElementType = new PointerType(referencedStructType.memberTypes().get(indexList.get(i)), false);
                } else {
                    throw new RuntimeException("When calcResultElementType(), the " + i + " layer of pointerOperand " + pointerOperand +
                            " is not a pointerType.");
                }
            }
            return resultElementType;
        } else {
            throw new RuntimeException("When calcResultElementType(), the type of pointerOperand is not PointerType. Got " + pointerOperand);
        }
    }

    @Override
    public String llvmStr(LLVMStrRegCounter counter) {
        StringBuilder sb = new StringBuilder();
        sb.append(counter.get(this));
        sb.append(" = getelementptr inbounds ");
        sb.append(((PointerType) this.getOperand(0).type()).referenceType().llvmStr()).append(" ");
        sb.append(this.getOperand(0).type().llvmStr()).append(" ");
        sb.append(counter.get(this.getOperand(0))).append(" ");
        for (Integer index : indexList) {
            sb.append(", i32 ");
            sb.append(index);
        }
        return sb.toString();
    }
}
