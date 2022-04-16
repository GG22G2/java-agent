import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.bytecode.*;
import javassist.bytecode.analysis.Analyzer;
import javassist.bytecode.analysis.Frame;


import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;


/**
 * @author 胡帅博
 * @date 2022/4/12 23:03
 */
public class PreMainTraceAgent {
    public static void premain(String agentArgs, Instrumentation ins) {
        System.out.println("agentArgs:" + agentArgs);
        ins.addTransformer(new DefineTransformer(), true);
    }

    public static class DefineTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

            if (className == null) {
                // 返回null表示不修改类字节码，和返回classfileBuffer是一样的效果。
                return null;
            }
            if (className.equals("App")) {
                ClassPool classPool = ClassPool.getDefault();
                classPool.appendClassPath(new LoaderClassPath(loader));
                classPool.appendSystemPath();
                try {
                    CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    CtMethod[] declaredMethods = ctClass.getDeclaredMethods();
                    for (CtMethod declaredMethod : declaredMethods) {
                        List<Integer> lines = analysisInsertPosition(declaredMethod);
                        for (Integer line : lines) {
                            declaredMethod.insertAt(line, "System.out.println(Thread.currentThread().getName()+\"执行了第" + line + "行代码,代码内容为:\");");
                        }

                    }
                    return ctClass.toBytecode();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return classfileBuffer;
        }

        public List<Integer> analysisInsertPosition(CtMethod declaredMethod) throws BadBytecode {

            List<Integer> line = new ArrayList<Integer>();


            Analyzer analyzer = new Analyzer();
            Frame[] codeFrame = analyzer.analyze(declaredMethod);

            MethodInfo methodInfo = declaredMethod.getMethodInfo();
            CodeAttribute CodeAttr = methodInfo.getCodeAttribute();
            AttributeInfo tempAttr = CodeAttr.getAttribute("LineNumberTable");
            if (tempAttr != null) {
                LineNumberAttribute lineNumberTable = (LineNumberAttribute) tempAttr;
                for (int i = 0; i < lineNumberTable.tableLength(); i++) {
                    int zijiemaIndex = lineNumberTable.startPc(i);
                    //获取栈深度
                    int topIndex = codeFrame[zijiemaIndex].getTopIndex();
                    line.add(lineNumberTable.toLineNumber(zijiemaIndex));
          /*          if (topIndex == -1) { //topIndex==-1时，栈中没有元素，应该上一行代码是;结尾的，可以在这行代码前插入内容
                        //System.out.println("代码行号:" + lineNumberTable.toLineNumber(zijiemaIndex));
                        if (i < lineNumberTable.tableLength() - 1) {
                           // line.add(lineNumberTable.toLineNumber(zijiemaIndex));
                            //System.out.println("字节码行号范围" + zijiemaIndex + "--" + (lineNumberTable.startPc(i + 1) - 1));
                        } else {
                            //System.out.println("字节码行号范围" + zijiemaIndex + "--" + zijiemaIndex);
                            //line.add(lineNumberTable.toLineNumber(zijiemaIndex)); //return语句
                        }

                    } else {
                        *//**
                         * 类似下边的结构，按照一行处理，
                         * Strin abc = "abc"
                         *             +"def";
                         * *//*
                        //System.out.println("代码行号:" + lineNumberTable.toLineNumber(zijiemaIndex) + ",该行是上一个语句的继续，不是新行，直接跳过");
                    }*/
                }
            }
            return line;
        }


    }
}
