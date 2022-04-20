import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.analysis.Analyzer;
import javassist.bytecode.analysis.Frame;
import javassist.compiler.CompileError;
import javassist.compiler.Javac;
import org.benf.cfr.reader.state.DCCommonState;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author 胡帅博
 * @date 2022/4/12 23:03
 */
public class PreMainTraceAgent {
    public static void premain(String agentArgs, Instrumentation ins) {
        ins.addTransformer(new DefineTransformer(), true);

    }

    public static class DefineTransformer implements ClassFileTransformer {
        DCCommonState dcCommonState;

        public DefineTransformer() {

        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            if (dcCommonState == null && loader != null) {
                dcCommonState = CFRHelper.getDCCommonState(loader);
            }
            if (className == null || dcCommonState == null) {
                // 返回null表示不修改类字节码，和返回classfileBuffer是一样的效果。
                return null;
            }

            if (className.equals("org/example/App")||className.equals("org/example/App$1")) {
                ClassPool classPool = ClassPool.getDefault();
                classPool.appendClassPath(new LoaderClassPath(loader));
                classPool.appendSystemPath();
                try {
                    CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    //List<String> innerClassList = getInnerClassList(ctClass);

                    CFRHelper.MethodInfo methodInfo = CFRHelper.parse(classfileBuffer, className, loader, dcCommonState);
                    Map<Integer, String> codes = methodInfo.codes;
                    Map<String, Map<Integer, Integer>> locs = methodInfo.locs;
                    CtMethod[] declaredMethods = ctClass.getDeclaredMethods();
                    for (CtMethod declaredMethod : declaredMethods) {
                        String methodId = declaredMethod.getName() + declaredMethod.getMethodInfo().getDescriptor();

                        Map<Integer, Integer> methodLocData = locs.get(methodId);

                        List<LineTableInfo> lines = analysisInsertPosition(declaredMethod);
                        Map<Integer, Integer> linePrintCount = new HashMap<Integer, Integer>();

                        Map<Integer, String> lineCode = getLineCode(lines, methodLocData, codes);

                        for (int i = lines.size() - 1; i >= 0; i--) {
                            LineTableInfo lineTableInfo = lines.get(i);
                            Integer line = lineTableInfo.javaLine;
                            Integer PrintCount = linePrintCount.getOrDefault(line, 0);

                            String code = lineCode.get(line);
                            if (code != null) {
                                code = convertCode(code);
                            }
                            try {
                                insertAt(lineTableInfo.classLineStart, true
                                        , "System.out.println(Thread.currentThread().getName()+\"," + className + "," + line + "行,代码:" + code + "\");"
                                        , declaredMethod, ctClass);
                            } catch (CannotCompileException e2) {
                                System.out.println("编译失败了:" + code);
                                //   e2.printStackTrace();
                            }
                            //System.out.println(Thread.currentThread().getName()+"执行了类org/example/App的第43行代码,代码内容为:        System.out.println(".");");


                        }
                    }
                    return ctClass.toBytecode();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return classfileBuffer;
        }

        public void insertAt(int codeArrayIndex, boolean modify, String src, CtMethod ctMethod, CtClass declaringClass)
                throws CannotCompileException {
            MethodInfo methodInfo = ctMethod.getMethodInfo();
            CodeAttribute ca = methodInfo.getCodeAttribute();
            if (ca == null)
                throw new CannotCompileException("no method body");

            LineNumberAttribute ainfo
                    = (LineNumberAttribute) ca.getAttribute(LineNumberAttribute.tag);


            int index = codeArrayIndex;
            CtClass cc = declaringClass;

            CodeIterator iterator = ca.iterator();
            Javac jv = new Javac(cc);
            try {
                jv.recordLocalVariables(ca, index);
                jv.recordParams(ctMethod.getParameterTypes(),
                        Modifier.isStatic(ctMethod.getModifiers()));
                jv.setMaxLocals(ca.getMaxLocals());
                jv.compileStmnt(src);
                Bytecode b = jv.getBytecode();
                int locals = b.getMaxLocals();
                int stack = b.getMaxStack();
                ca.setMaxLocals(locals);

                /* We assume that there is no values in the operand stack
                 * at the position where the bytecode is inserted.
                 */
                if (stack > ca.getMaxStack())
                    ca.setMaxStack(stack);

                index = iterator.insertAt(index, b.get());
                iterator.insert(b.getExceptionTable(), index);
                methodInfo.rebuildStackMapIf6(cc.getClassPool(), cc.getClassFile2());
            } catch (NotFoundException e) {
                throw new CannotCompileException(e);
            } catch (CompileError e) {
                throw new CannotCompileException(e);
            } catch (BadBytecode e) {
                throw new CannotCompileException(e);
            }
        }

        public String convertCode(String code) {
            code = code.replace("\\", "\\\\");
            code = code.replace("\"", "\\\"");
            return code;
        }

        public Map<Integer, String> getLineCode(List<LineTableInfo> lines, Map<Integer, Integer> methodLocData, Map<Integer, String> codes) {

            Map<Integer, String> result = new HashMap<Integer, String>();

            if (methodLocData == null || codes == null || lines.size() == 0) {
                return result;
            }

            for (Map.Entry<Integer, Integer> integerIntegerEntry : methodLocData.entrySet()) {
                Integer key = integerIntegerEntry.getKey();
                Integer value = integerIntegerEntry.getValue();

                for (LineTableInfo line : lines) {
                    if (line.in(key)) {
                        String lineCode = codes.get(value);
                        result.put(line.javaLine, lineCode);
                    }
                }

            }
            return result;
        }


        public List<String> getInnerClassList(CtClass ctClass) {
            List<String> innerClassList = new ArrayList<String>();
            ClassFile classFile = ctClass.getClassFile();
            AttributeInfo innerClasses = classFile.getAttribute("InnerClasses");
            if (innerClasses != null) {
                InnerClassesAttribute innerClassesAttribute = (InnerClassesAttribute) innerClasses;
                int length = innerClassesAttribute.tableLength();
                for (int i = 0; i < length; i++) {
                    String innerClassName = innerClassesAttribute.innerClass(i);
                    innerClassList.add(innerClassName);
                }
            }
            return innerClassList;
        }

        public static class LineTableInfo {
            public int javaLine;
            public int classLineStart;
            public int classLineEnd;

            public LineTableInfo(int javaLine, int classLineStart, int classLineEnd) {
                this.javaLine = javaLine;
                this.classLineStart = classLineStart;
                this.classLineEnd = classLineEnd;
            }

            public boolean in(int line) {
                return line >= classLineStart && line <= classLineEnd;
            }


            @Override
            public String toString() {
                return "LineTableInfo{" +
                        "javaLine=" + javaLine +
                        ", classLineStart=" + classLineStart +
                        ", classLineEnd=" + classLineEnd +
                        '}';
            }
        }

        public List<LineTableInfo> analysisInsertPosition(CtMethod declaredMethod) {
            List<LineTableInfo> line = new ArrayList<LineTableInfo>();
            Analyzer analyzer = new Analyzer();
            try {
                Frame[] codeFrame = analyzer.analyze(declaredMethod);
                MethodInfo methodInfo = declaredMethod.getMethodInfo();
                CodeAttribute CodeAttr = methodInfo.getCodeAttribute();
                if (CodeAttr != null) {
                    AttributeInfo tempAttr = CodeAttr.getAttribute("LineNumberTable");
                    if (tempAttr != null) {
                        LineNumberAttribute lineNumberTable = (LineNumberAttribute) tempAttr;
                        for (int i = 0; i < lineNumberTable.tableLength() - 1; i++) {
                            int zijiemaIndex = lineNumberTable.startPc(i);
                            //获取栈深度
                            int topIndex = codeFrame[zijiemaIndex].getTopIndex();
                            if (topIndex == -1) { //topIndex==-1时，栈中没有元素，应该上一行代码是;结尾的，可以在这行代码前插入内容
                                LineTableInfo lineTableInfo = new LineTableInfo(lineNumberTable.toLineNumber(zijiemaIndex), zijiemaIndex, lineNumberTable.startPc(i + 1) - 1);
                                line.add(lineTableInfo);
                            }
                        }
                    }
                }

            } catch (BadBytecode e) {
                e.printStackTrace();
            }
            return line;
        }


    }
}
