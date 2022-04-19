package test;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.*;
import javassist.bytecode.analysis.Analyzer;
import javassist.bytecode.analysis.Frame;

/**
 * @author 胡帅博
 * @date 2022/4/15 21:37
 */
public class javassistTest {
    public static void main(String[] args) throws NotFoundException, BadBytecode {
        ClassPool classPool = ClassPool.getDefault();
        CtClass ctClass = classPool.get("org.example.App");

        CtMethod[] declaredMethods = ctClass.getDeclaredMethods();

        for (CtMethod declaredMethod : declaredMethods) {
            Analyzer analyzer = new Analyzer();
            Frame[] codeFrame = analyzer.analyze(declaredMethod);

            MethodInfo methodInfo = declaredMethod.getMethodInfo();
            CodeAttribute CodeAttr = methodInfo.getCodeAttribute();
            AttributeInfo tempAttr = CodeAttr.getAttribute("LineNumberTable");



            if (tempAttr != null) {
                LineNumberAttribute lineNumberTable = (LineNumberAttribute) tempAttr;
                //  System.out.println(lineNumberTable.tableLength());
                //输入字节码行号，返回代码行号
                //  System.out.println(lineNumberTable.toLineNumber(1));
                //输入函数第i行的行号，返回实际在java文件中的行号
                //   System.out.println(lineNumberTable.lineNumber(0));
                //     System.out.println(lineNumberTable.startPc(0));
                //   System.out.println(lineNumberTable.toStartPc(15));

                for (int i = 0; i < lineNumberTable.tableLength(); i++) {
                    int zijiemaIndex = lineNumberTable.startPc(i);
                    //获取栈深度
                    int topIndex = codeFrame[zijiemaIndex].getTopIndex();

                    if (topIndex == -1) { //topIndex==-1时，栈中没有元素，应该上一行代码是;结尾的，可以在这行代码前插入内容
                      //  System.out.println("代码行号:"+lineNumberTable.toLineNumber(zijiemaIndex));
                        if (i<lineNumberTable.tableLength()-1){
                        //    System.out.println("字节码行号范围"+zijiemaIndex+"--"+(lineNumberTable.startPc(i+1)-1));
                        }else {
                       //     System.out.println("字节码行号范围"+zijiemaIndex+"--"+zijiemaIndex);
                        }
                    }else {
                     //   System.out.println("代码行号:"+lineNumberTable.toLineNumber(zijiemaIndex)+",该行是上一个语句的继续，不是新行，直接跳过");
                    }
                }
            }

//            tempAttr = CodeAttr.getAttribute("LocalVariableTable");
//            if (tempAttr != null) {
//                LocalVariableAttribute   localVariableTable =  (LocalVariableAttribute) tempAttr;
//                System.out.println(localVariableTable);
//            }
            System.out.println("代码长度"+CodeAttr.getCodeLength());
            CodeIterator iterator = CodeAttr.iterator();
            while (iterator.hasNext()) {
                int Code = iterator.next();
            }

        }
    }
}
