package pass.refactor;

import IR.IRModule;
import IR.IRUser;
import IR.IRValue;
import IR.type.IRType;
import IR.type.IntegerType;
import IR.value.IRBasicBlock;
import IR.value.IRFunction;
import IR.value.constant.ConstantInt;
import IR.value.instruction.*;
import pass.Pass;
import util.DoublyLinkedList;
import util.Pair;

import java.util.*;

public class Mem2Reg implements Pass {
    private final IRModule irModule;
    private boolean finished = false;

    private final HashSet<AllocaInst> localVariables;
    private final HashMap<PHINode, AllocaInst> phiMap;

    public Mem2Reg(IRModule irModule) {
        this.irModule = irModule;
        this.localVariables = new HashSet<>();
        this.phiMap = new HashMap<>();
    }

    @Override
    public void run() {
        if (finished) {
            throw new RuntimeException("When Mem2Reg.run(), the pass is finished");
        }
        for (IRFunction irFunction : irModule.functions()) {
            if (!irFunction.isLib()) {
                this.localVariables.clear();
                this.phiMap.clear();
                this.insertPhi(irFunction);
                this.renameVariable(irFunction);
            }
        }
        this.finished = true;
    }

    private void insertPhi(IRFunction irFunction) {
        IRBasicBlock defBlock = irFunction.basicBlocks().get(1);
        // 只处理局部非数组变量
        for (DoublyLinkedList.Node<IRInstruction<?>> instructionNode : defBlock.instructions()) {
            if (instructionNode.value() instanceof AllocaInst allocaInst &&
                    (IRType.isEqual(allocaInst.allocatedType(), IRType.getInt8Ty()) ||
                            IRType.isEqual(allocaInst.allocatedType(), IRType.getInt32Ty()))) {
                this.localVariables.add(allocaInst);
            }
        }
        for (AllocaInst variable : this.localVariables) {
            HashSet<IRBasicBlock> phiBasicBlocks = new HashSet<>();
            HashSet<IRBasicBlock> originDefBasicBlocks = new HashSet<>();
            LinkedList<IRBasicBlock> defBasicBlocksQueue = new LinkedList<>();
            for (IRUser<?> user : variable.users()) {
                if (user instanceof StoreInst storeInst) {
                    originDefBasicBlocks.add(storeInst.parent());
                    defBasicBlocksQueue.offer(storeInst.parent());
                }
            }
            while (!defBasicBlocksQueue.isEmpty()) {
                IRBasicBlock defBasicBlock = defBasicBlocksQueue.pop();
                for (IRBasicBlock frontier : defBasicBlock.dominanceFrontiers()) {
                    if (!phiBasicBlocks.contains(frontier)) {
                        // CAST 上方只处理局部非数组变量确保正确
                        PHINode phiNode = new PHINode((IntegerType) variable.allocatedType(), frontier);
                        this.phiMap.put(phiNode, variable);
                        phiBasicBlocks.add(frontier);
                        if (!originDefBasicBlocks.contains(frontier)) {
                            defBasicBlocksQueue.offer(frontier);
                        }
                    }
                }
            }
        }
    }

    private void renameVariable(IRFunction irFunction) {
        HashMap<IRBasicBlock, Boolean> dfsVisit = new HashMap<>();
        LinkedList<Pair<IRBasicBlock, HashMap<AllocaInst, IRValue<?>>>> dfsStack = new LinkedList<>();
        irFunction.basicBlocks().forEach(block -> dfsVisit.put(block, false));
        // 对于未赋值就使用变量的值是不确定的，给0也是合理的
        HashMap<AllocaInst, IRValue<?>> defaultReachingDefs = new HashMap<>();
        localVariables.forEach(variable -> defaultReachingDefs.put(variable, ConstantInt.ZERO_I32()));
        // 跳过参数块和定义块开始处理
        dfsStack.push(new Pair<>(irFunction.basicBlocks().get(0), defaultReachingDefs));
        dfsVisit.put(irFunction.basicBlocks().get(0), true);
        while (!dfsStack.isEmpty()) {
            Pair<IRBasicBlock, HashMap<AllocaInst, IRValue<?>>> dfsNowPair = dfsStack.pop();
            IRBasicBlock currentBasicBlock = dfsNowPair.key();
            HashMap<AllocaInst, IRValue<?>> reachingDefs = dfsNowPair.value();
            Iterator<DoublyLinkedList.Node<IRInstruction<?>>> instructionIterator = currentBasicBlock.instructions().iterator();
            while (instructionIterator.hasNext()) {
                DoublyLinkedList.Node<IRInstruction<?>> instructionNode = instructionIterator.next();
                if (instructionNode.value() instanceof PHINode phiNode) {
                    reachingDefs.put(this.phiMap.get(phiNode), phiNode);
                } else if (instructionNode.value() instanceof StoreInst storeInst &&
                        storeInst.getPointerOperand() instanceof AllocaInst storeAllocaInst &&
                        this.localVariables.contains(storeAllocaInst)) {
                    reachingDefs.put(storeAllocaInst, storeInst.getValueOperand());
                    storeInst.dropAllOperands();
                    // StoreInst没有User，可以直接删除
                    instructionIterator.remove();
                } else if (instructionNode.value() instanceof LoadInst loadInst &&
                        loadInst.getPointerOperand() instanceof AllocaInst loadAllocaInst &&
                        this.localVariables.contains(loadAllocaInst)) {
                    loadInst.replaceAllUsesWith(reachingDefs.get(loadAllocaInst));
                    loadInst.dropAllOperands();
                    // LoadInst的User已经被全部替换，可以直接删除
                    instructionIterator.remove();
                }
            }
            for (IRBasicBlock successor : currentBasicBlock.successors()) {
                for (DoublyLinkedList.Node<IRInstruction<?>> instructionNode : successor.instructions()) {
                    if (instructionNode.value() instanceof PHINode phiNode) {
                        phiNode.addIncoming(reachingDefs.get(this.phiMap.get(phiNode)), currentBasicBlock);
                    }
                }
                if (!dfsVisit.get(successor)) {
                    dfsStack.push(new Pair<>(successor, new HashMap<>(reachingDefs)));
                    dfsVisit.put(successor, true);
                }
            }
        }
        for (AllocaInst allocaInst : this.localVariables) {
            allocaInst.eliminate();
        }
    }
}
