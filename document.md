# 编译器设计文档

[TOC]

## 一 编译器总体设计

### 1. 总体结构

本编译器采用经典的三端设计，分为前端(frontend)、中端(middle)、后端(backend)三个部分。

前端的主要工作是词法分析、语法分析、语义分析生成中间代码(LLVM IR)，这三个阶段都要进行错误处理。

中端的主要工作是进行中端优化。

后端的主要工作是生成目标代码(MIPS汇编)、进行后端优化。

### 2. 接口设计

本编译器采用的是分别进行编译的各个步骤，通过多个Pass完成源代码到目标代码的编译工作。Compiler.java文件清晰地显示了数据的流向和各趟的执行内容。

#### 2.1 总体

```java
public static void main(String[] args) {
  // 根据传入的参数调整全局设置，用于本地的测试
  Config.setConfigByArgs(args);
  try {
    // 源代码文件作为输入
    SourceCode sourceCode = new SourceCode(Config.inputFilename);
    // 前端解析源代码文件
    frontend(sourceCode);
    // 关闭源代码文件
    sourceCode.close();
  } catch (IOException e) {
    e.printStackTrace();
  }
}
```

#### 2.2 前端

```java
private static void frontend(SourceCode sourceCode) throws IOException {
  // 创建前端所需的错误表
  ErrorTable errorTable = new ErrorTable();
  // Stage1 词法分析
  // 创建Lexer -> 得到TokenStream -> 关闭文件 -> 错误处理 -> [输出TokenStream]
  Lexer lexer = new Lexer(sourceCode.reader(), errorTable);
  TokenStream tokenStream = lexer.getTokenStream();
  if (Config.dumpTokenStream) {
    DumpTokenStream.dump(tokenStream.getArrayListCopy());
  }
  // Stage2 语法分析
  // 创建CompUnit(AST) -> 错误处理 -> [输出CompUnit(AST)]
  CompUnit compUnit = new CompUnit(tokenStream);
  // 如果有错误，在语法分析输出前即停止
  errorHandle(errorTable);
  if (Config.dumpAST) {
    DumpAST.dump(compUnit);
  }
  return;
}

private static void errorHandle(ErrorTable errorTable) throws IOException {
  if (errorTable.notEmpty()) {
    DumpErrorTable.dump(errorTable);
    exit(1);
  }
}
```

### 3. 文件组织

#### 3.1 `input`-输入

```
input
└── SourceCode.java ===> 源代码文件作为输入
```

#### 3.2 `global`-全局

```
global
└── Config.java ===> 全局配置文件
```

#### 3.3 `frontend`-前端

```
frontend
├── error ===> 错误处理
│   ├── ErrorRecord.java ===> 错误记录
│   ├── ErrorTable.java ===> 错误表
│   └── ErrorType.java ===> 错误类型
├── lexer ===> 词法分析
│   ├── Lexer.java ===> 词法分析器
│   ├── Token.java ===> 词素
│   └── TokenStream.java ===> 词素流
├── parser ===> 语法分析，其中的各文件与非终结符对应
│   ├── CompUnit.java ===> 顶层CompUnit
│   ├── declaration ===> 声明相关
│   │   ├── Decl.java
│   │   ├── MainFuncDef.java
│   │   ├── constant
│   │   │   ├── ConstDecl.java
│   │   │   ├── ConstDef.java
│   │   │   └── ConstInitVal.java
│   │   ├── function
│   │   │   ├── FuncDef.java
│   │   │   ├── FuncFParam.java
│   │   │   ├── FuncFParams.java
│   │   │   └── FuncType.java
│   │   └── variable
│   │       ├── InitVal.java
│   │       ├── VarDecl.java
│   │       └── VarDef.java
│   ├── expression ===> 表达式相关
│   │   ├── AddExp.java
│   │   ├── Character.java
│   │   ├── Cond.java
│   │   ├── ConstExp.java
│   │   ├── EqExp.java
│   │   ├── Exp.java
│   │   ├── FuncRParams.java
│   │   ├── LAndExp.java
│   │   ├── LOrExp.java
│   │   ├── LVal.java
│   │   ├── MulExp.java
│   │   ├── Number.java
│   │   ├── PrimaryExp.java
│   │   ├── RelExp.java
│   │   ├── UnaryExp.java
│   │   └── UnaryOp.java
│   └── statement ===>语句相关
│       ├── Block.java
│       ├── BlockItem.java
│       ├── ForStmt.java
│       └── Stmt.java
└── type ===> 前端公用的类型声明
    ├── ASTNode.java ===> 抽象语法树结点
    ├── ASTNodeOption.java ===> 抽象语法树结点的选项（如Stmt中的if）
    ├── ASTNodeWithOption.java ===> 带有选项的抽象语法数结点（如Stmt）
    └── TokenType.java ===> 词素类型
```

#### 3.4 `output`-输出

```
output
├── DumpAST.java ===> 输出抽象语法树
├── DumpErrorTable.java ===> 输出错误表
└── DumpTokenStream.java ===> 输出词素流
```

## 二 词法分析设计

### 1. 设计

使用`PushbackReader`作为读取源代码的工具，该Reader支持退回已读取的字符，适用于在分析符号时可能需要往前看一个字符的情况。将各类解析分别编成方法放入`Lexer`中，利用Java的`Character`类提供的一些方法。词法分析器同时携带一个词素流(`TokenStream`)，用于后续的语法分析的时候读取。对于其中的每个词素，使用`record`定义。包括需要打印的词素的类型和原始字符串值。

### 2. 实现

#### 2.1 `Lexer`词法分析器

由于Java提供的`Character`采用的是UTF-8编码，故需要重写一些判断方法。同时为了配合`PushbackReader`的使用，也需要提供一些功能方法

```java
private static boolean isDigit(char ch) {}
private static boolean isLetter(char ch) {}
private static boolean isLetterOrDigit(char ch) {}
private void fgetc() throws IOException {}
private void ungetc() throws IOException {}
```

词法分析器约定每次完成某个读取过程总是预读好了一个字符（存入`c`）以供判断。根据读到的字符调用相应的函数进行处理

```java
public TokenStream getTokenStream() throws IOException {
  // 如果已经完成生成TokenStream，直接返回结果
  if (finish) {
    return this.stream;
  }
  fgetc();
  // 约定为每一次循环结束之后都保证c为预读好的一个字符
  while (c != EOF) {
    if (c == '\n') {
      this.newLine(); // 记录行号
      fgetc();
    } else if (c == ' ' || c == '\t' || c == '\f' || c == '\r') {
      fgetc(); // 跳过空白符号
    } else if (c == '_' || isLetter(c)) {
      this.lexIdentKeyword(); // 标识符或关键字
    } else if (isDigit(c)) {
      this.lexIntConst(); // 整型数字常量
    } else if (c == '"') {
      this.lexStringConst(); // 字符串常量
    } else if (c == '\'') {
      this.lexCharConst(); // 字符常量
    } else {
      this.lexSymbolComment(); // 各种符号或注释，若为未知字符则直接跳过
    }
  }
  // 加入类型为EOF的Token，表示TokenStream结束
  this.gotToken(TokenType.EOF, "");
  this.finish = true;
  return this.stream;
}
```

对于需要处理的错误，根据要求记录错误并退回字符

```java
case '|' -> {
  fgetc();
  if (c == '|') {
    this.gotToken(TokenType.OR, "||");
  } else {
    this.gotToken(TokenType.OR, "|");
    this.errorTable.addErrorRecord(this.line, ErrorType.ILLEGAL_AND_OR,
            "Got '" + c + "'(ASCII:" + (int) c + ") when expected '|'");
    ungetc();
  }
}
```

#### 2.2 `Token`词素

为了调试和后续错误处理定位行号和位置方便。除了要输出的类型和原始字符串值之外同时记录了所在行的行号和在该行的位置。

```java
public record Token(TokenType type, String strVal, int line, int indexOfLine) {}
```

## 三 语法分析设计

### 1. 设计

将`TokenStream`从`Lexer`中独立出来，作为`Lexer`产生的数据传输给语法分析部分。语法分析部分采用递归下降子程序法组织，各个作为抽象语法树结点的对应非终结符的类的构造方法作为递归下降的子程序。每个类的对象根据文法的要求保存子结点或者词素。若一个非终结符的候选式有多个差距较大的，则分别构建不同的类。完成构建抽象语法树后，使用后序遍历方式输出整棵语法树。

### 2. 实现

#### 2.1 `TokenStream`词素流

为了方便进行获取指定类型的词素，如果不是指定的词素返回空或抛出异常或记录错误这个需要多次进行的操作，在词素流中增加多个类型的`consume`方法。

```java
// 如果当前指向的Token为types中指定的类型，则返回并且指针向后移动一位
// 否则返回null
public Token consumeOrNull(TokenType... types) {}
// 如果当前指向的Token为types中指定的类型，则返回并且指针向后移动一位
// 否则根据情况决定是否抛出错误
public Token consumeOrThrow(String place, TokenType... types) {}
// 如果当前指向的Token为types中指定的类型，则返回并且指针向后移动一位
// 否则登记到错误处理表中
public Token consumeOrError(String place, ErrorType errorType, TokenType... types) {}
```

#### 2.2 `ASTNodeWithOption`有多个候选式的非终结符

为了方便有多个候选式的非终结符的输出。将不同的候选式作为对应非终结符的类的内部类，并实现相关的接口。同时通过抽象类定义这样的非终结符对应的类的行为。

```java
public abstract class ASTNodeWithOption<T extends ASTNodeOption> {
  private final T option;
  protected ASTNodeWithOption(T option) {
    this.option = option;
  }
  public T extract() { // 通过extract()方法可以解出其中包含的选项内容
    return this.option;
  }
}
```

```java
public class UnaryExp extends ASTNodeWithOption<UnaryExp.UnaryExpOption> {
  private UnaryExp(UnaryExpOption option) {
    super(option);
  }
  public interface UnaryExpOption extends ASTNodeOption {}
  static UnaryExp parse(TokenStream stream) {
    if (stream.isNow(TokenType.PLUS, TokenType.MINU, TokenType.NOT)) {
      return new UnaryExp(new UnaryExp_UnaryOp(stream));
    } else if ......
  }
  public static class UnaryExp_PrimaryExp implements UnaryExpOption {}
}
```

#### 2.3 `DumpAST`后序遍历输出语法树

每个非终结符对应的类提供`explore`方法，返回这一层所有的结点。如果是有多个候选式的非终结符，还需要调用`extract`方法。遍历到非终结符的则递归，遍历到词素则输出。因为类的名字与非终结符的名字是对应的，所以可以使用`getClass`方法获得对象的类后直接输出类名。注意有多个候选式的不应输出内部类名。

```java
private static void dump(ArrayList<Object> nodes) throws IOException {
  for (Object node : nodes) {
    if (node != null) {
      if (node instanceof ASTNode branchNode) {
        dump(branchNode.explore());
        out.write("<" + branchNode.getClass().getSimpleName() + ">" + "\n");
      } else if (node instanceof ASTNodeWithOption<?> branchNodeWithOption) {
        dump(branchNodeWithOption.extract().explore());
        out.write("<" + branchNodeWithOption.getClass().getSimpleName() + ">" + "\n");
      } else if (node instanceof Token leafNode) {
        out.write(leafNode.type().toString() + " " + leafNode.strVal() + "\n");
      }
    }
  }
}
```

## 四、语义分析

### 1. 设计

考虑到我的路径是经过语义分析之后生成LLVM IR，随后再转换为MIPS汇编码。语义分析过程势必与LLVM IR的生成过程强相关，将两个过程联合进行完成显然比先完成了语义分析得到了符号表，而不考虑其他应该产生的信息，再在后续的工作中加入要好。在语义分析的文档当中将主要讲述与语义分析作业相关的内容。

语义分析主要由四部分组成。第一部分是符号表系统(SymbolTable)，由于符号表的首要目的是为后续生成LLVM IR提供参考，符号表中填写的元素要考虑到LLVM IR生成的需求。第二部分是Visitor，用于深度优先访问语法分析过程当中得到的语法树，并且完成对符号表的操作、对LLVM IR的生成的操作和对异常情况的处理。第三部分是Character，用于编译器静态分析ConstExp，而不是像在Visitor当中那样动态地生成LLVM IR的代码。第四部分是Translator，用于将词法分析器得到的单词转换为分析过程中所期望的数据格式，尤其是解析CharConst和StringConst。

### 2. 实现

#### 2.1 符号表

Symbol是符号表的元素的公用抽象类，具体分为ConstSymbol、VarSymbol、ArgSymbol和FuncSymbol。这四种符号在语义分析中要进行不同的操作。Symbol Table中使用栈式符号表实现符号的查询和插入，并且使用链表持久化存储用于符号表的输出。符号的未定义和重定义都只会发生在查询符号表的时候，统一在SymbolTable报出相关错误。

```java
public abstract class Symbol<T extends IRType, VT extends IRValue<?>> {
    private final T type;
    private final String name;
    private final int line;
    private VT irValue = null;	
    ......
}
```

```java
public class SymbolTable {
    private final ErrorTable errorTable;

    private final LinkedList<HashMap<String, Symbol<?, ?>>> subTableStack;
    private final ArrayList<ArrayList<Symbol<?, ?>>> symbolList;
    private final LinkedList<Integer> subSymbolListIndexStack;

    ......

    boolean insert(Symbol<?, ?> newSymbol) {
        ......
            if (currentSubTable.containsKey(newSymbol.name())) {
                errorTable.addErrorRecord(newSymbol.line(), ErrorType.DUPLICATED_IDENT,
                        "Duplicated symbol '" + newSymbol.name() + "' at line " + newSymbol.line() + ", " +
                                "last defined at line " + currentSubTable.get(newSymbol.name()).line());
        ......
    }

    Symbol<?, ?> searchOrError(Token ident) {
        Symbol<?, ?> ret = this.searchOrNull(ident);
        if (ret == null) {
            errorTable.addErrorRecord(ident.line(), ErrorType.UNDEFINED_IDENT,
                    "Undefined symbol '" + ident.strVal() + "', referenced at line " + ident.line());

        }
        return ret;
    }
}
```

#### 2.2 Visitor

Visitor当中的模式与递归下降类似，由和某个非终结符对应的子程序调用子程序解析子节点，逐层递归。对于各类错误的检查在相关的子程序当中完成。Visitor不生成LLVM IR，只做基本块的传递，主要是用于定义变量的入口基本块的传递和当前要将新的LLVM Instruction插入到的基本块当中。同时针对for循环，通过构建基本块栈的形式实现break和continue的判断和定位。

```java
public class Visitor {
  private final CompUnit compUnit;
  private boolean finish = false;

  private final SymbolTable symbolTable;
  private final Calculator calculator;
  private final Builder builder;
  private final LinkedList<BasicBlock> forEndBlocks;
  private final LinkedList<BasicBlock> forTailBlocks;

  private final ErrorTable errorTable;

  private final IRModule irModule;
  ......

  private IRValue<?> visitLValEvaluation(LVal lVal, BasicBlock insertBlock) {
    // LVal做evaluation，可能的返回的类型有int, char, int*, char*
    Symbol<?, ?> searchedSymbol = this.symbolTable.searchOrError(lVal.ident());
    if (searchedSymbol instanceof FuncSymbol) {
      throw new RuntimeException("When visitLValEvaluation(), the search result of " + lVal.ident() + " is a function");
    } else if (searchedSymbol instanceof ConstSymbol || searchedSymbol instanceof VarSymbol || searchedSymbol instanceof ArgSymbol) {
      // CAST 上方的instanceof确保转换正确
      IRValue<PointerType> lValAddress = IRValue.cast(searchedSymbol.irValue());
      }
      if (lVal.getType() == LVal.Type.BASIC && lValAddress.type().referenceType() instanceof IntegerType) {
        // 变量、常量
        return this.builder.loadLVal(lValAddress, insertBlock);
      }
      ......
    } ......
  }

  private BasicBlock visitStmtIf(Stmt.Stmt_If stmt_if, BasicBlock entryBlock, BasicBlock nowBlock) {
    BasicBlock ifBodyBlock = this.builder.newBasicBlock();
    BasicBlock ifEndBlock = this.builder.newBasicBlock();
    if (stmt_if.elseStmt() == null) {
      // 无else语句
      this.visitCond(stmt_if.cond(), ifBodyBlock, ifEndBlock, nowBlock);
      // if语句体
      this.builder.appendBasicBlock(ifBodyBlock);
      BasicBlock ifBodyLastBlock = this.visitStmt(stmt_if.ifStmt(), entryBlock, ifBodyBlock);
      this.builder.addBranchInstruction(null, ifEndBlock, null, ifBodyLastBlock);
    } ......
    ......
  }
}
```

## 附录 参考编译器介绍

选择的参考编译器是PL/0编译器。PL/0编译器是一个经典的编译教学用编译器，采用Pascal语言编写，其可编译的高级语言PL/0是一种类似于Pascal的简单语言。以下是PL/0的文法定义：

```
# 程序
program = block "."

# 代码块，由常量定义、变量定义和多个过程定义组成
block = [ "const" ident "=" number {"," ident "=" number} ";"] # 常量定义一定给初始值
        [ "var" ident {"," ident} ";"] # 变量定义一定没有初始值
        { "procedure" ident ";" block ";" } # 过程（代码块）可以嵌套
        statement

statement = [ ident ":=" expression # 赋值语句
              | "call" ident # 调用过程语句
              | "begin" statement {";" statement } "end" # 多条语句的开始和结束
              | "if" condition "then" statement  # if语句（不支持else）
              | "while" condition "do" statement # while语句
              | "read" "(" identifier {"," identifier} ")" # 输入语句，读入值到变量
              | "write" "(" expression {"," expression} ")" # 输出语句，输入表达式的值
            ] # 语句可以为空

condition = "odd" expression | # 是否是奇数
            expression ("="|"<>"|"<"|"<="|">"|">=") expression # 大小关系（<>是不等于）

expression = ["+"|"-"] term { ("+"|"-") term} # 表达式

term = factor {("*"|"/") factor} # 项

factor = ident | number | "(" expression ")"; # 因子

# ident仅由小写字母和数字构成，且不能以数字开头
# number没有限制不能以0开头，均为十进制整型数字
```

### 1. 总体结构

PL/0编译器包括了词法分析、语法分析、语义分析、错误处理、中间代码生成、虚拟机执行中间代码等过程。使用的是语法分析驱动词法分析的模式，并且采用递归下降分析、语法制导翻译，对于源代码只需要进行一遍扫描即可完成生成中间代码。对于分析过程中的错误将在分析过程中输出。若分析过程当中没有遇到问题，则在分析完成之后执行生成的中间代码。生成的中间代码类似于PCODE，解释运行在栈式虚拟机上，具体的中间代码种类如下：

```assembly
lit 0, a : load constant a # 加载立即数a到栈顶  
opr 0, a : execute operation a # 取出栈顶与次栈顶（或者仅栈顶）并进行运算a，并将结果压入栈顶
lod l, a : load variable l,a # 加载内存l(evel), a(ddr)处的变量到栈顶，此处的a(ddr)是相对于l(evel)的AR基地址的偏移值
sto l, a : store variable l,a # 把栈顶写入到l(evel), a(ddr)处的变量并退栈，此处的a(ddr)是相对于l(evel)的AR基地址的偏移值
cal l, a : call procedure a at level l # 调用l(evel), a(ddr)的过程，分配AR并设置其中的值，但是不移动栈顶，此处的a(ddr)是代码的绝对地址
int 0, a : increment t-register by a # 栈顶寄存器加立即数a以移动栈顶置针
jmp 0, a : jump to a # 无条件跳转到a(ddr)，此处的a(ddr)是代码的绝对地址
jpc 0, a : jump conditional to a # 栈顶为0时跳转到a(ddr)并退栈，此处的a(ddr)是代码的绝对地址
red l, a : read variable l,a # 读入一个数值，赋值给l(evel), a(ddr)处的变量，此处的a(ddr)是相对于l(evel)的AR基地址的偏移值
wrt 0, 0 : write stack-top # 输出栈顶的数值并退栈
```

### 2. 接口设计

PL/0编译器采用了语法分析驱动词法分析的模式，语法分析采用递归下降分析，并且采用语法制导翻译生成中间代码。语法分析的入口为block过程。在其中将不断调用getsym过程进行词法分析。翻译生成的中间代码调用gen过程进行保存。分析过程中遇到错误调用error过程进行记录并输出错误信息。

错误处理过程需要判断词法分析得到的token是否为合理的类型，如果为不合理的类型需要跳过一定的字符到置顶的边界符号类型以继续分析程序剩下的错误。为此PL/0编译器特地设计了test过程，其接受s1和s2两个符号类型集合和一个错误代码。如果当前符号是s1当中的符号类型，即通过测试。如果不是，则不断跳过符号直到当前符号类型是是s2当中的符号类型，并且报出指定代码的错误。而在语法分析的递归下降过程中，各过程都接收一个名为fsys的参数，该参数根据调用的深入而不断扩展，将用于传递给s1或s2。

### 3. 文件组织

PL/0编译器将编译器的各个部分分解为多个过程，其中过程之间的数据采用全局常量、全局变量配合全局类型进行交换。

#### 1) 全局常量

| 名称   | 值   | 含义                                            |
| ------ | ---- | ----------------------------------------------- |
| norw   | 13   | 保留字的数量                                    |
| txmax  | 100  | 标识符表的长度                                  |
| nmax   | 14   | 数字的最大长度                                  |
| al     | 10   | 标识符的最大长度                                |
| amax   | 2047 | PCODE中a的最大值，限制的是lit指令的立即数的大小 |
| levmax | 3    | PCODE中l的最大值，限制的是block的最大嵌套层次   |
| cxmax  | 200  | 生成中间代码的最大条数（实际为201条）           |

#### 2) 全局类型

| 名称        | 类型     | 含义                                                         |
| ----------- | -------- | ------------------------------------------------------------ |
| symbol      | 枚举     | PL/0程序中所有符号种类，包括标识符、数字、运算和关系符号、保留字等 |
| alfa        | 字符数组 | 存储标识符名称字符串                                         |
| objecttyp   | 枚举     | 标识符标识的对象的种类，包括常量、变量和过程                 |
| symset      | 集合     | 元素为符号种类（*symbol*）的集合                             |
| fct         | 枚举     | 中间代码所有的指令码                                         |
| instruction | 记录     | 中间代码，f字段为指令码，l字段为block嵌套层次，a字段为地址偏移值 |

#### 3) 全局变量

| 名称       | 类型                            | 含义                             |
| ---------- | ------------------------------- | -------------------------------- |
| ch         | char                            | 当前正在分析的字符               |
| sym        | *symbol*                        | 最后一个分析出的符号种类         |
| id         | *alfa*                          | 最后一个分析出的标识符或保留字   |
| num        | integer                         | 最后一个分析出的数字             |
| cc         | integer                         | 当前行已参与分析的字符计数       |
| ll         | integer                         | 当前行的总字符数                 |
| kk         | integer                         | 最后一个分析到的标识符的长度     |
| err        | integer                         | 总错误计数                       |
| cx         | integer                         | 当前中间代码数组的下标（条数）   |
| line       | 字符数组                        | 当前行的读取缓冲区               |
| a          | *alfa*                          | 存放将进行分析的字符串的临时数组 |
| code       | *instruction*数组               | 存储生成的中间代码的数组         |
| word       | *alfa数组*                      | 保留字的名称字符串数组           |
| wsym       | *symbol*数组                    | 保留字的符号种类数组             |
| ssym       | *symbol*数组                    | 单个字符对应的符号种类数组       |
| mnemonic   | 二维字符数组                    | 中间代码指令码的名称字符串数组   |
| declbegsys | *symset*                        | 声明部分的起始符号种类           |
| statbegsys | *symset*                        | 语句部分的起始符号种类           |
| facbegsys  | *symset*                        | 因子的起始符号种类               |
| table      | 记录数组（记录内容随objecttyp） | 符号表                           |
| fin        | text                            | 源程序文件                       |
| sfile      | string                          | 源程序文件名                     |

#### 4) 错误处理

```pascal
procedure error( n : integer ); {错误报告过程，n为错误码}
  begin
    {将^移动到出错的符号处，并且报告错误码为n}
    {之所以输出四个*是为了和输出中间代码行数的时候固定宽度为4配合}
    writeln( '****', ' ':cc-1, '^', n:2 );
    {总错误计数+1}
    err := err+1
  end; { error }
```

#### 5) 词法分析

```pascal
procedure getsym; {词法分析过程，获取一个token}
  var i,j,k : integer;
  procedure getch; {更新当前正在分析的字符}
    begin
      if cc = ll  { get character to end of line } {如果已经分析完了当前行的最后一个字符，读入下一行到缓冲区}
        then begin { read next line }
          if eof(fin) {如果调用了这个过程，但是已经文件已经读取完成了，那么说明程序是不完整的}
            then begin
              {报错，关闭打开的文件并且退出程序}
              writeln('program incomplete');
              close(fin);
              exit;
            end;
          ll := 0; {重置当前行的总字符数}
          cc := 0; {重置当前行已参与分析的字符数}
          write(cx:4,' ');  { print code address } {输出当前中间代码已经生成了多少行}
          while not eoln(fin) do {读取直到End Of Line}
            begin
              ll := ll+1; {更新当前行的总字符数}
              read(fin,ch); {从文件中读入一个字符fin，此处ch借用来存储从文件中读取的字符}
              write(ch); {控制台输出刚读取到的字符ch}
              line[ll] := ch {更新当前行缓冲区的内容}
            end;
          writeln; {控制台输出一个换行}
          readln(fin); {读取文件的一行，实际是读入剩余的换行符}
          ll := ll+1; {当前行的总字符数+1，当cc的值增长到该值时说明读到了换行符，即该行已分析完}
          line[ll] := ' ' { process end-line } {换行符在缓冲区中用空格代替}
        end;
      {如果没有分析完当前行，则从缓冲区中取出下一字符}
      cc := cc+1; {更新当前行已参与分析的字符数}
      ch := line[cc] {更新当前正在分析的字符}
    end; { getch }
  begin { procedure getsym } {主过程开始}
    {确保总数预先读入了一个字符}
    while ch = ' ' do {跳过所有的空白符}
      getch;
    if ch in ['a'..'z'] {如果开头是a-z之间，要么是标识符，要么是保留字}
      then begin  { identifier of reserved word }
        k := 0; {临时变量，此处记录已经读过的字符序列的长度}
        repeat {不断地进行读取，直到读取到的字符不属于标识符合法的字符范围}
          if k < al {如果已经读过的字符序列小于标识符的最大长度}
            then begin
              k := k+1; {长度自增}
              a[k] := ch {存入临时数组准备后续进行分析}
            end;
          getch {读下一个字符，如果已经读过的字符序列长度已经超过了标识符的最大长度，也要继续读取直到读完这个标识符，但是直接忽略后面的内容}
          until not( ch in ['a'..'z','0'..'9'] );
        if k >= kk        { kk : last identifier length }
          then kk := k {如果本次读过的字符序列长度大于等于上一次的长度，说明上一次读取在a中写入的字符已经完全被覆盖了}
        else repeat {如果不是，说明上一次读取的字符没有被完全覆盖，要用空格覆盖掉}
          a[kk] := ' ';
          kk := kk-1
          until kk = k;
        id := a; {将临时数组的值给到id即最后一个分析到的标识符或保留字}
        i := 1; {初始化二分查找所要用到的临时变量}
        j := norw;   { binary search reserved word table }
        repeat {在保留字的名称字符串表中进行二分查找}
          k := (i+j) div 2; {这里的二分查找不太一样，每次都会对两个边界进行判断，而不是最多更新一个边界}
          if id <= word[k]
            then j := k-1;
          if id >= word[k]
            then i := k+1
          until i > j;
        if i-1 > j {如果存在一个匹配的，那么最后一次循环时左边界和右边界都会更新，导致i和j相差不是1，所以这个是找到的条件。如果没有匹配的，那么最后一次只会更新一边，相差就是1了}
          then sym := wsym[k] {找到了，设置最后一个分析到符号种类为保留字对应的符号种类}
        else sym := ident {没有找到，设置最后一个分析到符号种类为标识符}
      end
    else if ch in ['0'..'9'] {如果开头是0-9之之间，那么一定是数字}
      then begin  { number }
        k := 0; {临时变量，此处记录已经读过的数字序列的长度}
        num := 0; {初始化最后一个分析到的数字为0}
        sym := number; {设置最后一个分析到符号种类为数字}
        repeat
          num := 10*num+(ord(ch)-ord('0')); {直接计算得到整数的结果，ord获取一些类型的值的整型表示，此处为字符对应的ASCII码值}
          k := k+1;
          getch
          until not( ch in ['0'..'9'] );
        if k > nmax {读完整个数字如果数字的长度超过了最大的长度，要报错}
          then error(30)
      end
    else if ch = ':' {如果开头是:，判断是否是:=}
      then begin
        getch;
        if ch = '=' {如果确实是=，那么就是赋值号}
          then begin
            sym := becomes;
            getch
          end
        else sym := nul {如果不是=，那么这个:是无效的，直接忽略掉}
      end
    else if ch = '<' {如果开始是<，判断是<、<=、<>中的哪一个}
      then begin
        getch;
        if ch = '=' {小于等于}
          then begin
            sym := leq;
            getch
          end
        else if ch = '>' {不等于}
          then begin
            sym := neq;
            getch
          end
        else sym := lss {小于}
      end
    else if ch = '>' {如果开头是>，判断是>、>=中的哪一个}
      then begin
        getch;
        if ch = '='
          then begin
            sym := geq;
            getch
          end
        else sym := gtr
      end
    else begin {其余的字符，判断该字符是否对应了某种符号类型，如果没有对应的种类，由于在主过程中初始化ssym时全部置了nul，直接忽略掉}
        sym := ssym[ch];
        getch
      end
  end; { getsym }
```

#### 6) 记录中间代码

```pascal
procedure gen( x : fct; y,z : integer ); {中间代码记录过程，x是指令码，y为l，z为a}
  begin
    if cx > cxmax {如果当前中间代码的条数已经超过了最大条数}
      then begin
        writeln('program too long'); {输出错误信息}
        close(fin); {关闭文件}
        exit {退出程序}
      end;

    with code[cx] do {使用with语句后，可以直接访问这个记录的字段，而不需要重复写出code[cx]}
      begin
        f := x;
        l := y;
        a := z
      end;
    cx := cx+1 {中间代码数组下标自增}
  end; { gen }
```

#### 7) 测试当前符号

```pascal
procedure test( s1,s2 : symset; n : integer ); {出现错误后跳读处理过程，s1是期望种类，s2是停止种类，n为错误码}
  begin
    if not ( sym in s1 ) {如果最后一个分析出的符号种类不是s1指定的符号种类}
      then begin
        error(n); {报告指定的错误}
        s1 := s1+s2; 
        while not( sym in s1 ) do {不断地进行读取，直到最后一个分析出的符号种类是s1和s2指定的种类}
          getsym
      end
  end; { test }
```

#### 8) 语法分析和中间代码生成

```pascal
procedure block( lev,tx : integer; fsys : symset ); {解析一个代码块的过程，lev为嵌套层次}
  var  dx : integer;  { data allocation index } {当前活动记录可以分配地址的编号}
       tx0: integer;  { initial table index } {代码块所属的过程在符号表下标}
       cx0: integer;  { initial code index } {开始解析代码块语句部分时中间代码数组下标}

  procedure enter( k : objecttyp ); {将标识符插入到符号表中，k为对象类型}
    begin { enter object into table }
      tx := tx+1; {符号表的下标增加1，注意符号表的第0条用户搜索，不存入实际信息}
      with table[tx] do
        begin
          name := id; {名字为最后一个分析出的标识符}
          kind := k; {对象类型}
          case k of
            constant: begin {常量}
                        if num > amax {如果最后一个分析出的数字超过了PCODE指令的a的最大值即立即大小的限制}
                          then begin
                            error(30); {报告错误}
                            num := 0 {将读取到的数字置为0}
                          end;
                          val := num {将读取到的数字存入符号表}
                      end;
            variable: begin {变量}
                        level := lev; {嵌套层次}
                        adr := dx; {分配的地址编号}
                        dx := dx+1 {可以分配地址的编号自增}
                      end;
            prosedure: level := lev; {过程，其标识符嵌套层次为当前层次}
          end
        end
    end; { enter }

  function position ( id : alfa ): integer; {找到标识符对应的符号表条目的下标}
    var i : integer;
    begin
      table[0].name := id; {先将要搜索的标识符存到符号表的第0条}
      i := tx; {从最后一条往前搜索}
      while table[i].name <> id do
        i := i-1; {如果不相等就一直往前搜索}
      position := i {返回值为停止时的下标，因为第一条被设置为了要搜索的标识符，所以搜索到了第0条就一定会停止，如果返回结果为0那么就是未搜索到}
    end;  { position }
    
  procedure constdeclaration; {解析常量声明}
    begin
      if sym = ident
      then begin
        {因为分析出的标识符会存储在id中，所以不用担心覆盖的问题}
        getsym; {获取下一个token}
        if sym in [eql,becomes] {符合语义的符号种类是=和:=}
          then begin
            if sym = becomes
              then error(1); {如果是:=，报错应该为=，但是因为符合语义所以继续处理}
            getsym;
            if sym = number
              then begin
                enter(constant); {将常量加入到符号表中}
                getsym {预读取下一个token}
              end
            else error(2) {没有正确地初始化一个常量，应当使用数字进行初始化}
          end
        else error(3) {没有正确地初始化一个常量，标识符后面应该有=}
      end
      else error(4) {有const，但是没有标识符}
    end; { constdeclaration }
    
  procedure vardeclaration; {解析变量声明}
    begin
      if sym = ident
      then begin
        enter(variable); {将变量加入到符号表中}
        getsym {预读取下一个token}
      end
      else error(4) {有var，但是没有标识符}
    end; { vardeclaration }
    
  procedure listcode; {输出中间代码}
    var i : integer;
    begin
      {输出从当前代码块语句部分开始到结束的中间代码}
      for i := cx0 to cx-1 do {因为中间代码数组是使用下标为0的元素的，所以要减1}
        with code[i] do
          writeln( i:4, mnemonic[f]:7,l:3, a:5) {按照格式输出中间代码行号、指令码、level和addr}
    end; { listcode }
    
  procedure statement( fsys : symset ); {解析语句}
    var i,cx1,cx2: integer;
    procedure expression( fsys : symset ); {解析表达式}
      var addop : symbol; {("+"|"-")}
      procedure term( fsys : symset); {解析项} {解析因子}
        var mulop: symbol ; {("*"|"/")}
        procedure factor( fsys : symset ); 
          var i : integer;
          begin
            test( facbegsys, fsys, 24 ); {测试是否是因子的开始符号种类，以传入本过程的边界符号种类为终止跳过符号，如果不是报错表达式不能以此符号开始}
            while sym in facbegsys do {如果是因子开头就不断进行分析}
              begin
                if sym = ident {标识符}
                  then begin
                    i := position(id); {临时变量，此处记录标识符在符号表中的下标}
                    if i= 0 {未找到，报错未声明的标识符}
                      then error(11)
                    else
                      with table[i] do
                        case kind of
                          constant : gen(lit,0,val); {常量，生成加载立即数指令}
                          variable : gen(lod,lev-level,adr); {变量，生成加载指定变量的内存指令}
                          prosedure: error(21) {报错，不能向过程赋值}
                        end;
                    getsym {预读一个token}
                  end
                else if sym = number
                  then begin
                    if num > amax {如果数字超过了PCODE指令的a的最大值即立即大小的限制}
                      then begin
                        error(30); {报错}
                        num := 0 {将读取到的数字置为0}
                      end;
                    gen(lit,0,num); {生成加载立即数指令}
                    getsym {预读一个token}
                  end
                else if sym = lparen
                  then begin
                    getsym; {读取下一个token}
                    expression([rparen]+fsys); {解析表达式，边界符号补充一种右括号}
                    if sym = rparen {完成括号嵌套的表达式的解析的过程，判断是否有右括号}
                      then getsym
                    else error(22)
                  end;
                test(fsys,[lparen],23) {测试解析完成因子后是否为边界符号，以左括号作为终止跳过符号，如果不是报错因为后不能为此符号}
              end
          end; { factor }
        begin { procedure term( fsys : symset);   
                var mulop: symbol ;    }
          factor( fsys+[times,slash] ); {解析第一个factor，边界符号增加times和slash}
          while sym in [times,slash] do
            begin
              mulop := sym; {乘号或者除号}
              getsym; {读取下一个token}
              factor( fsys+[times,slash] ); {解析一个factor，边界符号增加times和slash}
              if mulop = times
                then gen( opr,0,4 ) {乘号，生成乘法运算指令}
              else gen( opr,0,5 ) {除号，生成除法运算指令}
            end
        end; { term }
      begin { procedure expression( fsys: symset);  
              var addop : symbol; }
        if sym in [plus, minus] {是否有开头的正号和负号}
          then begin
            addop := sym; {读取开头的正号和负号}
            getsym; {读取下一个token}
            term( fsys+[plus,minus] ); {解析第一个term，边界符号增加plus和minus}
            if addop = minus {如果是正号，那么可以忽略}
              then gen(opr,0,1) {如果是负号，那么要生成取反指令}
          end
        else term( fsys+[plus,minus] ); {没有开头的正号或者负号，那么直接解析第一个term，边界符号增加plus和minus}
        while sym in [plus,minus] do {有加号或者减号，一直读入term}
          begin
            addop := sym; {加号或者减号}
            getsym; {读取下一个token}
            term( fsys+[plus,minus] ); {解析一个term，边界符号增加plus和minus}
            if addop = plus
              then gen( opr,0,2 ) {加号，生成加法运算指令}
            else gen( opr,0,3 ) {减号，生成减法运算指令}
          end
      end; { expression }
      
    procedure condition( fsys : symset ); {解析关系表达式}
      var relop : symbol; {("="|"<>"|"<"|"<="|">"|">=")}
      begin
        if sym = oddsym {"odd" expression}
          then begin
            getsym; {读取下一个token}
            expression(fsys); {读取表达式}
            gen(opr,0,6) {生成运算指令}
          end
        else begin
          expression( [eql,neq,lss,gtr,leq,geq]+fsys); {读取左侧的表达式，边界符号增添各类关系运算符}
          if not( sym in [eql,neq,lss,leq,gtr,geq]) {判断是否是合法的关系运算符}
            then error(20)
          else begin
            relop := sym; {关系运算符}
            getsym; {预读一个token}
            expression(fsys); {读取右侧的表达式}
            case relop of {根据关系运算符生成运算指令}
              eql : gen(opr,0,8);
              neq : gen(opr,0,9);
              lss : gen(opr,0,10);
              geq : gen(opr,0,11);
              gtr : gen(opr,0,12);
              leq : gen(opr,0,13);
            end
          end
        end
      end; { condition }
    begin { procedure statement( fsys : symset );  
            var i,cx1,cx2: integer; }
      if sym = ident {ident ":=" expression}
        then begin
          i := position(id); {临时变量，此处记录标识符在符号表中的下标}
          if i = 0
             then error(11) {未找到，报错未声明的标识符}
          else if table[i].kind <> variable {判断是否为变量}
            then begin { giving value to non-variation }
              error(12); {报错只能向变量赋值}
              i := 0 {将找到的下标置为0，等同于未找到}
            end;
          getsym; {读入下一个token}
          if sym = becomes {是否是赋值符号}
             then getsym {读入下一个token}
          else error(13); {报错应当有赋值符号}
          expression(fsys); {解析一个表达式}
          if i <> 0 {如果标识符是找到了的（而且是合法的）}
            then
              with table[i] do
                gen(sto,lev-level,adr) {生成存储到指定变量的内存的指令，表达式的计算结果一定保存在栈顶}
              end
      else if sym = callsym {"call" ident}
        then begin
          getsym; {读入下一个token}
          if sym <> ident {判断call后面是否有标识符}
            then error(14)
          else begin
            i := position(id); {临时变量，此处记录标识符在符号表中的下标}
            if i = 0
              then error(11) {未找到，报错未声明的标识符}
            else
              with table[i] do
                if kind = prosedure {判断是否为过程}
                  then gen(cal,lev-level,adr) {生成调用过程指令}
                else error(15); {报错只能调用过程}
                getsym {预读一个token}
              end
           end
      else if sym = ifsym {"if" condition "then" statement}
        then begin
          getsym; {读入下一个token}
          condition([thensym,dosym]+fsys); {读入条件}
          if sym = thensym {判断call后面是否有then}
            then getsym
          else error(16);
          cx1 := cx; {临时变量，保存当前中间代码数组的下标}
          gen(jpc,0,0); {先生成一条栈顶为0时的跳转指令，用于跳转到if条件不满足时的跳转的地方，解析完if条件满足时的语句部分之后再填写地址}
          statement(fsys); {解析语句部分}
          code[cx1].a := cx {填写if条件不满足时要跳转到的地址}
        end
      else if sym = beginsym
        then begin
          getsym; {读入下一个token}
          statement([semicolon,endsym]+fsys); {读入一条语句}
          while sym in ([semicolon]+statbegsys) do {在读入完一条语句的结尾是分号或者是语句的开始符号类型时持续读入语句}
            begin
              if sym = semicolon {分号}
                then getsym {读入下一个token}
              else error(10); {报错，语句的结尾应为分号}
              statement([semicolon,endsym]+fsys) {读入一条语句}
            end;
          if sym = endsym {读入完成所有的语句之后判断是否有end}
            then getsym {预读一个token}
          else error(17) {报错应该有end}
        end
      else if sym = whilesym
        then begin
          cx1 := cx; {临时变量，保存while语句条件开始前中间代码数组的下标}
          getsym; {读取下一个token}
          condition([dosym]+fsys); {读入条件}
          cx2 := cx; {临时变量，保存while语句循环体开始前中间代码数组的下标}
          gen(jpc,0,0); {先生成一条栈顶为0时的跳转指令，用于跳转到while条件不满足时的跳转的地方，解析完while条件满足时的语句部分之后再填写地址}
          if sym = dosym {判断条件后后面是否有do}
            then getsym
          else error(18);
          statement(fsys); {解析语句部分}
          gen(jmp,0,cx1); {跳转到条件开始前，形成循环}
          code[cx2].a := cx {填写while条件不满足时要跳转到的地址}
        end
      else if sym = readsym
        then begin
          getsym; {读取下一个token}
          if sym = lparen {是否有左括号}
            then
              repeat
                getsym; {读取下一个token}
                if sym = ident {是否是标识符}
                  then begin
                    i := position(id); {临时变量，此处记录标识符在符号表中的下标}
                    if i = 0
                      then error(11) {未找到，报错未声明的标识符}
                    else if table[i].kind <> variable {判断是否为变量}
                      then begin
                        error(12); {报错只能向变量赋值}
                        i := 0 {将找到的下标置为0，等同于未找到}
                      end
                    else with table[i] do
                      gen(red,lev-level,adr) {存储到指定变量的内存的指令}
                  end
                else error(4); {报错read后应该为标识符}
                getsym; {读取下一个token}
              until sym <> comma {有逗号，继续读取要读入数值的变量}
          else error(40); {报错没有左括号}
          if sym <> rparen {解析完成需要读入的变量的序列，判断是有右括号}
            then error(22);
          getsym {预读一个token}
        end
      else if sym = writesym
        then begin
          getsym; {读取下一个token}
          if sym = lparen {是否有左括号}
            then begin
              repeat
                getsym; {读取下一个token}
                expression([rparen,comma]+fsys); {读取表达式，边界符号补充右括号和逗号}
                gen(wrt,0,0); {生成输出栈顶值的指令}
              until sym <> comma; {有逗号，继续读取要输出的表达式}
              if sym <> rparen {解析完成需要输出的表达式的序列，判断是有右括号}
                then error(22);
              getsym
            end
          else error(40) {报错没有左括号}
        end;
      test(fsys,[],19) {测试语句后的符号是为边界符号，不是就一直跳过，并报错语句后的符号不正确}
    end; { statement }
  begin { procedure block( lev,tx : integer; fsys : symset );   
            var  dx : integer;  /*data allocation index*/
                 tx0: integer;  /*initial table index*/
                 cx0: integer;  /*initial code index*/              }
    dx := 3; {前个空间用于存放display（静态链）、prev abp（动态链）、ret addr（返回值）}
    tx0 := tx; {保存代码块所属的过程在符号表下标}
    table[tx].adr := cx; {保存开始解析代码块时中间代码数组的下标}
    gen(jmp,0,0); { jump from declaration part to statement part } {暂时生成一条没有地址的jmp指令，等到内部包含的过程处理完成之后再填写具体的地址以跳过嵌套定义的过程的中间代码，这条跳转指令更像是为了全局代码块而准备的}
    if lev > levmax
      then error(32); {嵌套层次太深}
    {处理常量、变量、嵌套过程的定义}
    repeat
      if sym = constsym
        {解析常量}
        then begin
          getsym; {预读一个token}
          repeat
            constdeclaration; {解析常量的声明}
            while sym = comma do {如果遇到逗号，继续解析下一个常量}
              begin
                getsym;
                constdeclaration
              end;
            if sym = semicolon {直到不是逗号，判断是否是分号，是分号则预读一个token}
              then getsym
            else error(5) {常量声明没有以分号结束}
          until sym <> ident {如果仍然是标识符从语义上理解为仍然有声明的常量，则继续解析常量}
        end;
      if sym = varsym
        {解析变量}
        then begin
          getsym; {预读一个token}
          repeat
            vardeclaration; {解析常量的声明}
            while sym = comma do
              begin
                getsym;
                vardeclaration
              end;
            if sym = semicolon {直到不是逗号，判断是否是分号，是分号则预读一个token}
              then getsym
            else error(5) {变量声明没有以分号结束}
          until sym <> ident; {如果仍然是标识符从语义上理解为仍然有声明的变量，则继续解析变量}
        end;
      while sym = procsym do
        {解析过程}
        begin
          getsym; {读入一个token}
          if sym = ident {如果读入的是一个标识符，那么将该过程加入到符号表中，并读入下一个token}
            then begin
              enter(prosedure);
              getsym
            end
          else error(4); {有procedure，但是没有标识符}
          if sym = semicolon {判断读入的是否是分号，如果是读入下一个token}
            then getsym
          else error(5); {过程定义中缺少标识符后面的分号}
          block(lev+1,tx,[semicolon]+fsys); {解析全局代码块，此时的嵌套层次+1，所属的过程在符号表下标为刚才加入符号表的符号，边界符号种类在传入本代码解析的边界符号种类基础上补充分号（嵌套过程定义结束）}
          if sym = semicolon {完成解析嵌套的过程，判断是否有分号}
            then begin
              getsym; {读入下一个token}
              test( statbegsys+[ident,procsym],fsys,6 ) {测试接下来的语句是否是语句部分的开始符号种类、标识符（赋值语句）、procedure声明；以传入本代码块解析的边界符号种类为终止跳过符号；测试失败报错过程说明说明结束后的符号种类不正确}
            end
          else error(5) {过程定义中缺少代码块后面的分号}
        end;
      test( statbegsys+[ident],declbegsys,7 ) {测试接下来的语句是否是语句部分的开始符号种类、标识符（赋值语句）；以声明部分的开始符号种类为终止跳过符号；测试失败报错嵌套过程声明全部完成之后应为语句部分}
    until not ( sym in declbegsys ); {不断解析声明部分，直到待分析的符号不是声明部分的开始符号种类}
    code[table[tx0].adr].a := cx;  { back enter statement code's start adr. } {填写生成的跳过嵌套定义的过程的中间代码的jmp指令的地址}
    with table[tx0] do
      begin
        adr := cx; { code's start address } {重新填写符号表中该代码对应过程的入口地址为语句部分开始的地方}
      end; 
    cx0 := cx; {保存开始解析代码块语句部分时中间代码数组下标，用于后续的代码输出}
    gen(int,0,dx); { topstack point to operation area } {向上移动栈顶指针，为代码块分配活动记录的空间}
    statement( [semicolon,endsym]+fsys ); {解析语句部分，边界符号种类在传入本代码解析的边界符号种类基础上补充分号和end}
    gen(opr,0,0); { return } {生成返回指令}
    test( fsys, [], 8 );
    listcode; {输出中间代码}
  end { block };
```

#### 9) 解释执行中间代码

```pascal
procedure interpret; {中间代码解释执行}
  const stacksize = 500; {栈式虚拟机的栈的大小}
  var p,b,t: integer; { program-,base-,topstack-register } {教程当中的PC、MP、SP三个寄存器}
      i : instruction;{ instruction register } {当前PC指向的指令}
      s : array[1..stacksize] of integer; { data store } {栈式虚拟机的栈}
  {该程序设计的每个活动记录的栈虚拟机从上到下分别为局部变量区、ret addr（返回值）、prev abp（动态链）、display（静态链）}
  function base( l : integer ): integer; {根据静态链找到往外l层的活动记录的基地址}
    var b1 : integer;
    begin { find base l levels down }
      b1 := b;
      while l > 0 do
        begin
          b1 := s[b1]; {因为基地址出对应的就是活动记录的静态链，所以可以通过循环跳到基地址处保存的值的方式往外l层}
          l := l-1
        end;
      base := b1
    end; { base }
  begin  
    writeln( 'START PL/0' ); {开始执行}
    t := 0; {初始时栈上没有数据，所以栈顶指针指向0}
    b := 1; {初始时的全局活动记录的基地址为1}
    p := 0; {程序计数器，开始时在第0条中间代码}
    s[1] := 0;
    s[2] := 0;
    s[3] := 0;
    repeat
      i := code[p]; {根据程序计数器获取当前指令}
      p := p+1; {程序计数器默认自增1}
      with i do
        case f of
          lit : begin
                {加载立即数a到栈顶}
                  t := t+1; {栈顶指针自增1}
                  s[t]:= a; {将立即数a存入栈顶}
                end;
          opr : case a of { operator } 
                {取出栈顶与次栈顶（或者仅栈顶）并进行运算a，并将结果压入栈顶}
                  0 : begin { return }
                        t := b-1; {回收当前活动记录的栈空间}
                        p := s[t+3]; {t+3对应的是回收的活动记录的ret addr}
                        b := s[t+2]; {t+2对应的是回收的活动记录的prev abp}
                      end;
                  1 : s[t] := -s[t]; {栈顶取反}
                  2 : begin {栈顶加次栈顶}
                        t := t-1;
                        s[t] := s[t]+s[t+1]
                      end;
                  3 : begin {栈顶减次栈顶}
                        t := t-1;
                        s[t] := s[t]-s[t+1]
                      end;
                  4 : begin {栈顶乘次栈顶}
                        t := t-1;
                        s[t] := s[t]*s[t+1]
                      end;
                  5 : begin {栈顶除以次栈顶}
                        t := t-1;
                        s[t] := s[t] div s[t+1]
                      end;
                  6 : s[t] := ord(odd(s[t])); {栈顶是否为奇数，ord获取布尔值的整型表示，True为1，False为0}
                  8 : begin {次栈顶是否等于栈顶}
                        t := t-1;
                        s[t] := ord(s[t]=s[t+1])
                      end;
                  9 : begin {次栈顶是否不等于栈顶}
                        t := t-1;
                        s[t] := ord(s[t]<>s[t+1])
                      end;
                  10: begin {次栈顶是否小于栈顶}
                        t := t-1;
                        s[t] := ord(s[t] < s[t+1])
                      end;
                  11: begin {次栈顶是否大于等于栈顶}
                        t := t-1;
                        s[t] := ord(s[t] >= s[t+1])
                      end;
                  12: begin {次栈顶是否大于栈顶}
                        t := t-1;
                        s[t] := ord(s[t] > s[t+1])
                      end;
                  13: begin {次栈顶是否小于等于栈顶}
                        t := t-1;
                        s[t] := ord(s[t] <= s[t+1])
                      end;
                end;
          lod : begin
                  {加载内存l(evel), a(ddr)处的变量到栈顶}
                  t := t+1;
                  s[t] := s[base(l)+a]
                end;
          sto : begin
                  {把栈顶写入到l(evel), a(ddr)处的变量并退栈}
                  s[base(l)+a] := s[t];
                  t := t-1
                end;
          cal : begin  { generate new block mark }
                  {l(evel), a(ddr)的过程，分配AR并设置其中的值，但是不移动栈顶}
                  s[t+1] := base(l); {新的活动记录的静态链}
                  s[t+2] := b; {新的活动记录的prev abp}
                  s[t+3] := p; {新的活动记录的ret addr}
                  b := t+1; {新的活动记录的基地址为当前栈顶+1}
                  p := a; {设置程序计数器为过程的入口地址}
                end;
          int : t := t+a; {栈顶寄存器加立即数a以移动栈顶置针}
          jmp : p := a; {无条件跳转到a(ddr)}
          jpc : begin
                  {栈顶为0时跳转到a(ddr)并退栈}
                  if s[t] = 0
                    then p := a;
                  t := t-1;
                end;
          red : begin
                  {读入一个数值，赋值给l(evel), a(ddr)处的变量}
                  writeln('??:'); {输出提示符}
                  readln(s[base(l)+a]);
                end;
          wrt : begin
                  {输出栈顶的数值并退栈}
                  writeln(s[t]);
                  t := t-1 {这里原来是t:=t+1}
                end
        end { with,case }
    until p = 0; {上来就会进入到主过程，所以p=0时，意味着程序退出了主过程也即结束}
    writeln('END PL/0'); {结束执行}
  end; { interpret }
```

#### 10) 主过程

```pascal
begin { main }
  writeln('please input source program file name : '); {提示输入源程序文件名}
  readln(sfile); {读取输入的文件名}
  assign(fin,sfile); {打开文件}
  reset(fin); {重置文件指针}
  for ch := 'A' to ';' do {初始化ssym，现将所有的符号种类都置为nul，以应对不合法的符号}
    ssym[ch] := nul;
  {初始化word，按照字典序排列，方便进行二分查找}
  word[1] := 'begin        '; word[2] := 'call         ';
  word[3] := 'const        '; word[4] := 'do           ';
  word[5] := 'end          '; word[6] := 'if           ';
  word[7] := 'odd          '; word[8] := 'procedure    ';
  word[9] := 'read         '; word[10]:= 'then         ';
  word[11]:= 'var          '; word[12]:= 'while        ';
  word[13]:= 'write        ';
  
  {初始化wsym，下标与word中的保留字的名称字符串下标对应}
  wsym[1] := beginsym;      wsym[2] := callsym;
  wsym[3] := constsym;      wsym[4] := dosym;
  wsym[5] := endsym;        wsym[6] := ifsym;
  wsym[7] := oddsym;        wsym[8] := procsym;
  wsym[9] := readsym;       wsym[10]:= thensym;
  wsym[11]:= varsym;        wsym[12]:= whilesym;
  wsym[13]:= writesym;
  
  {初始化ssym，下标与合法的单个字符符号对应}
  ssym['+'] := plus;        ssym['-'] := minus;
  ssym['*'] := times;       ssym['/'] := slash;
  ssym['('] := lparen;      ssym[')'] := rparen;
  ssym['='] := eql;         ssym[','] := comma;
  ssym['.'] := period;
  ssym['<'] := lss;         ssym['>'] := gtr;
  ssym[';'] := semicolon;
  
  {初始化mnemonic，下标与指令码对应}
  mnemonic[lit] := 'LIT  '; mnemonic[opr] := 'OPR  ';
  mnemonic[lod] := 'LOD  '; mnemonic[sto] := 'STO  ';
  mnemonic[cal] := 'CAL  '; mnemonic[int] := 'INT  ';
  mnemonic[jmp] := 'JMP  '; mnemonic[jpc] := 'JPC  ';
  mnemonic[red] := 'RED  '; mnemonic[wrt] := 'WRT  ';
  
  declbegsys := [ constsym, varsym, procsym ]; {声明部分的开始符号种类集合}
  statbegsys := [ beginsym, callsym, ifsym, whilesym]; {语句部分的开始符号种类集合，标识符不是所有情况都合法，read和write不允许单独作为语句的开始}
  facbegsys := [ ident, number, lparen ]; {因子的开始符号种类集合}
  err := 0; {初始化错误计数器}
  cc := 0; {初始化当前行已参与分析的字符数}
  cx := 0; {初始化中间代码的条数}
  ll := 0; {初始化当前行的总字符数}
  ch := ' '; {初始化当前正在分析的字符为空格，这样getsym时会调用getch}
  kk := al; {初始化最后一个分析到的标识符的长度为标识符的最大长度，这样读到的一个标识符会用空格覆盖所有未被使用的字符}
  getsym; {预读第一个token}
  {解析全局代码块，此时的嵌套层次为0，全局代码所属的过程在符号表下标为0，边界符号种类为声明部分、语句部分的开始符号种类集合和.（程序结束）}
  block( 0,0,[period]+declbegsys+statbegsys );
  if sym <> period {解析完了全局代码块，是否以.结束}
    then error(9); {程序没有以.结束}
  if err = 0 {如果没有错误}
    then interpret {解释执行}
  else write('ERRORS IN PL/0 PROGRAM');{如果有错误，输出错误提示}
  writeln; {换行}
  close(fin) {关闭文件}
end. 
```
