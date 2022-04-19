import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.mapping.MappingFactory;
import org.benf.cfr.reader.mapping.ObfuscationMapping;
import org.benf.cfr.reader.state.ClassFileSourceImpl;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollectingDumper;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.bytestream.BaseByteData;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.functors.BinaryFunction;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author 胡帅博
 * @date 2022/4/18 21:02
 */
public class CFRHelper {

    public static DCCommonState getDCCommonState(ClassLoader loader) {
        HashMap<String, String> optionMap = new HashMap<>();
        optionMap.put("trackbytecodeloc", "true");
        OptionsImpl options = new OptionsImpl(optionMap);

        ClassFileSourceImpl classFileSource = new ClassFileSourceImpl(options);
        classFileSource.informAnalysisRelativePathDetail(null, null);
        DCCommonState dcCommonState = new DCCommonState(options, classFileSource);

        //ObfuscationMapping mapping = MappingFactory.get(options, dcCommonState);
        //   dcCommonState = new DCCommonState(dcCommonState,mapping);
        dcCommonState = new DCCommonState(dcCommonState, new BinaryFunction<String, DCCommonState, org.benf.cfr.reader.entities.ClassFile>() {
            @Override
            public org.benf.cfr.reader.entities.ClassFile invoke(String innerClassName, DCCommonState arg2) {
                //System.out.println("找类" + innerClassName);
                org.benf.cfr.reader.entities.ClassFile classFile = null;
                try {
                    classFile = arg2.loadClassFileAtPath(innerClassName);
                    return classFile;
                } catch (Exception e) {
                    if (classFile == null) {

                        InputStream resourceAsStream = loader.getResourceAsStream(innerClassName);
                        if (resourceAsStream != null) {
                            byte[] bytes = null;
                            try {
                                bytes = new byte[resourceAsStream.available()];
                                int read = resourceAsStream.read(bytes);
                                return CFRHelper.getCLassFileFromBytes(bytes, innerClassName, arg2);
                            } catch (IOException e2) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (classFile == null) {
                        System.out.println("找不到类" + innerClassName);
                        throw new CannotLoadClassException(innerClassName, e);
                    }
                }
                System.out.println("找不到类" + innerClassName);
                throw new CannotLoadClassException(innerClassName, new Throwable());
            }
        });
        return dcCommonState;
    }


    public static MethodInfo parse(byte[] classFiles, String classPath, ClassLoader loader, DCCommonState dcCommonState) throws CannotLoadClassException {
        HashMap<String, String> optionMap = new HashMap<>();
        optionMap.put("trackbytecodeloc", "true");
        OptionsImpl options = new OptionsImpl(optionMap);

        InternalDumperFactoryImpl dumperFactory = new InternalDumperFactoryImpl(options);

        //从字节加载类信息
        ClassFile c = getCLassFileFromBytes(classFiles, classPath, dcCommonState);

        dcCommonState.configureWith(c);
        //分析类型
        dumperFactory.getProgressDumper().analysingType(c.getClassType());
        //这个地方重新获取ClassFile
        try {
            c = dcCommonState.getClassFile(c.getClassType());
        } catch (CannotLoadClassException ignore) {
        }
        //解析内部类
        c.loadInnerClasses(dcCommonState);

        TypeUsageCollectingDumper collectingDumper = new TypeUsageCollectingDumper(options, c);

        c.analyseTop(dcCommonState, collectingDumper);

        TypeUsageInformation typeUsageInformation = collectingDumper.getRealTypeUsageInformation();
        IllegalIdentifierDump illegalIdentifierDump = IllegalIdentifierDump.Factory.get(options);
        MovableDumperContext movableDumperContext = new MovableDumperContext();
        StdIODumperNoPrint d = new StdIODumperNoPrint(typeUsageInformation, options, illegalIdentifierDump, movableDumperContext);
        Dumper byteCoded = dumperFactory.wrapLineNoDumper(d);

        for (Method method : c.getMethods()) {
            method.dump(byteCoded, true);
        }
        d.close();

        //获取行号和字节码对照关系
        if (byteCoded instanceof BytecodeTrackingDumper) {
            Map<Method, BytecodeTrackingDumper.MethodBytecode> perMethod = ((BytecodeTrackingDumper) byteCoded).perMethod;

            Map<String, Map<Integer, Integer>> data = new HashMap<String, Map<Integer, Integer>>();


            for (Map.Entry<Method, BytecodeTrackingDumper.MethodBytecode> entry : perMethod.entrySet()) {
                final TreeMap<Integer, Integer> methodLocData = entry.getValue().getFinal();
                final Method method = entry.getKey();
                String methodSignature = method.getName() + method.getMethodPrototype().getOriginalDescriptor();
                data.put(methodSignature, methodLocData);
            }
            d.flush();

            MethodInfo methodInfo = new MethodInfo(d.getCodes(), data);
            return methodInfo;
            //行号和字节码对应关系
/*            for (BytecodeDumpConsumer.Item item : result) {
                for (Map.Entry<Integer, Integer> entry : item.getBytecodeLocs().entrySet()) {
                    System.out.println("Line " + entry.getValue() + "\t: " + entry.getKey());
                }
            }*/
        }

        return new MethodInfo(new HashMap<>(), new HashMap<>());
    }

    public static class MethodInfo {
        public Map<Integer, String> codes;
        public Map<String, Map<Integer, Integer>> locs;

        public MethodInfo(Map<Integer, String> codes, Map<String, Map<Integer, Integer>> locs) {
            this.codes = codes;
            this.locs = locs;
        }
    }

    public static ClassFile getCLassFileFromBytes(byte[] bytes, String classPath, DCCommonState dcCommonState) {
        Pair<byte[], String> content = Pair.make(bytes, classPath);
        ByteData data = new BaseByteData(content.getFirst());
        return new ClassFile(data, content.getSecond(), dcCommonState);
    }

}
