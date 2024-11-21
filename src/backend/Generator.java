package backend;

import IR.IRModule;
import backend.target.TargetModule;

public class Generator {
    private final IRModule irModule;
    private boolean finish = false;

    private final TargetModule targetModule;

    public Generator(IRModule irModule) {
        this.irModule = irModule;
        this.targetModule = new TargetModule();
    }

    public TargetModule generateTargetModule() {
        if (finish) {
            return this.targetModule;
        }

        this.finish = true;
        return this.targetModule;
    }
}
