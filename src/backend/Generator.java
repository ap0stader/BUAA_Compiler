package backend;

import IR.IRModule;
import backend.target.TargetModule;

public class Generator {
    private final IRModule irModule;

    public Generator(IRModule irModule) {
        this.irModule = irModule;
    }

    public TargetModule generateTargetModule() {
        return null;
    }
}
