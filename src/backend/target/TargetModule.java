package backend.target;

import java.util.ArrayList;

public class TargetModule {
    private final ArrayList<TargetDataObject> dataObjects;
    private final ArrayList<TargetFunction> functions;

    public TargetModule() {
        this.dataObjects = new ArrayList<>();
        this.functions = new ArrayList<>();
    }

    public ArrayList<TargetDataObject> dataObjects() {
        return dataObjects;
    }

    public ArrayList<TargetFunction> functions() {
        return functions;
    }

    public void appendDataObjects(TargetDataObject dataObject) {
        this.dataObjects.add(dataObject);
    }

    public void appendFunctions(TargetFunction function) {
        this.functions.add(function);
    }
}
