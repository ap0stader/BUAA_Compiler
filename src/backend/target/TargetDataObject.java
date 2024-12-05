package backend.target;

import IR.type.IntegerType;
import IR.value.constant.ConstantInt;

import java.util.ArrayList;
import java.util.LinkedList;

public class TargetDataObject {
    private final String name;
    private final LinkedList<Directive> directives;

    public TargetDataObject(String name) {
        this.name = name;
        this.directives = new LinkedList<>();
    }

    public void appendData(ConstantInt initInt) {
        if (initInt.type().size() == 8) {
            if (this.directives.isEmpty()
                    || !(this.directives.peekLast() instanceof DirectiveList)
                    || this.directives.peekLast().directiveType != DirectiveType.BYTE) {
                this.directives.add(new DirectiveList(DirectiveType.BYTE));
            }

        } else if (initInt.type().size() == 32) {
            if (this.directives.isEmpty()
                    || !(this.directives.peekLast() instanceof DirectiveList)
                    || this.directives.peekLast().directiveType != DirectiveType.WORD) {
                this.directives.add(new DirectiveList(DirectiveType.WORD));
            }
        } else {
            throw new RuntimeException("When appendData(), the size of initInt is invalid. " +
                    "Got " + initInt.type().size() + ", expected 8 or 32");
        }
        // CAST 上方的instanceof确保转换正确
        ((DirectiveList) directives.peekLast()).values.add(initInt.constantValue());
    }

    public void appendZero(IntegerType elementType, Integer repeats) {
        if (elementType.size() == 8) {
            this.directives.add(new DirectiveBundle(DirectiveType.BYTE, 0, repeats));
        } else if (elementType.size() == 32) {
            this.directives.add(new DirectiveBundle(DirectiveType.WORD, 0, repeats));
        } else {
            throw new RuntimeException("When appendZero(), the size of elementType is invalid. " +
                    "Got " + elementType.size() + ", expected 8 or 32");
        }
    }

    public String mipsStr() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": ");
        for (int i = 0; i < this.directives.size(); i++) {
            // 加入空格对齐伪指令
            sb.append(i > 0 ? "\n" + " ".repeat(this.name.length() + 2) : "");
            sb.append(this.directives.get(i).mipsStr());
        }
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return this.mipsStr();
    }

    private enum DirectiveType {
        WORD,
        BYTE;

        @Override
        public String toString() {
            return "." + this.name().toLowerCase();
        }
    }

    private static abstract class Directive {
        final DirectiveType directiveType;

        Directive(DirectiveType directiveType) {
            this.directiveType = directiveType;
        }

        abstract String mipsStr();

        @Override
        public String toString() {
            return this.mipsStr();
        }
    }


    private static class DirectiveList extends Directive {
        private final ArrayList<Integer> values;

        private DirectiveList(DirectiveType directiveType) {
            super(directiveType);
            this.values = new ArrayList<>();
        }

        @Override
        String mipsStr() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.directiveType).append(" ");
            for (int i = 0; i < this.values.size(); i++) {
                sb.append(i > 0 ? ", " : "");
                sb.append(this.values.get(i));
            }
            return sb.toString();
        }
    }

    private static class DirectiveBundle extends Directive {
        private final Integer value;
        private final Integer repeats;

        @SuppressWarnings("SameParameterValue")
        private DirectiveBundle(DirectiveType directiveType, Integer value, Integer repeats) {
            super(directiveType);
            this.value = value;
            this.repeats = repeats;
        }

        @Override
        String mipsStr() {
            return this.directiveType + " " + this.value + ":" + repeats;
        }
    }
}
