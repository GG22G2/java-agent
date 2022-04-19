package test;


import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.mapping.MappingFactory;
import org.benf.cfr.reader.mapping.ObfuscationMapping;
import org.benf.cfr.reader.state.ClassFileSourceImpl;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollectingDumper;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.*;

import java.util.*;

/**
 * @author 胡帅博
 * @date 2022/4/16 10:15
 */
public class CFRTest {
    public static void main1(String[] args) {
        HashMap<String, String> optionMap = new HashMap<>();
        optionMap.put("trackbytecodeloc", "true");
        OptionsImpl options = new OptionsImpl(optionMap);
        List<String> files = new ArrayList<>();
        String AppClassPath = "G:\\kaifa_environment\\code\\java\\java-agent\\target\\classes\\org.example.App.class";

        files.add(AppClassPath);

        CfrDriver cfrDriver = new CfrDriver.Builder().withBuiltOptions(options).build();
        cfrDriver.analyse(files);


    }


    public static void main(String[] args) {
        HashMap<String, String> optionMap = new HashMap<>();
        optionMap.put("trackbytecodeloc", "true");
        OptionsImpl options = new OptionsImpl(optionMap);
        List<String> files = new ArrayList<>();
        String AppClassPath = "G:\\kaifa_environment\\code\\java\\java-agent\\target\\classes\\org\\example\\App.class";

        ClassFileSourceImpl classFileSource = new ClassFileSourceImpl(options);
        classFileSource.informAnalysisRelativePathDetail(null, null);
        DCCommonState dcCommonState = new DCCommonState(options, classFileSource);
        InternalDumperFactoryImpl dumperFactory = new InternalDumperFactoryImpl(options);

        ObfuscationMapping mapping = MappingFactory.get(options, dcCommonState);
        dcCommonState = new DCCommonState(dcCommonState, mapping);

        ClassFile c = dcCommonState.loadClassFileAtPath(AppClassPath);
        dcCommonState.configureWith(c);
        dumperFactory.getProgressDumper().analysingType(c.getClassType());
        try {
            c = dcCommonState.getClassFile(c.getClassType());
        } catch (CannotLoadClassException ignore) {
        }
        if (options.getOption(OptionsImpl.DECOMPILE_INNER_CLASSES)) {
            c.loadInnerClasses(dcCommonState);
        }

        TypeUsageCollectingDumper collectingDumper = new TypeUsageCollectingDumper(options, c);

        c.analyseTop(dcCommonState, collectingDumper);

        TypeUsageInformation typeUsageInformation = collectingDumper.getRealTypeUsageInformation();
        IllegalIdentifierDump illegalIdentifierDump = IllegalIdentifierDump.Factory.get(options);


        StdIODumperNoPrint d = new StdIODumperNoPrint(typeUsageInformation, options, illegalIdentifierDump, new MovableDumperContext());
        Dumper byteCoded = dumperFactory.wrapLineNoDumper(d);

        for (Method method : c.getMethods()) {
            method.dump(byteCoded, true);
        }
        d.close();
        if (byteCoded instanceof BytecodeTrackingDumper) {
            Map<Method, BytecodeTrackingDumper.MethodBytecode> perMethod = ((BytecodeTrackingDumper) byteCoded).perMethod;
            List<BytecodeDumpConsumer.Item> result = new ArrayList<BytecodeDumpConsumer.Item>();
            for (Map.Entry<Method, BytecodeTrackingDumper.MethodBytecode> entry : perMethod.entrySet()) {
                final TreeMap<Integer, Integer> data = entry.getValue().getFinal();
                final Method method = entry.getKey();
                result.add(new BytecodeDumpConsumer.Item() {
                    @Override
                    public Method getMethod() {
                        return method;
                    }

                    @Override
                    public NavigableMap<Integer, Integer> getBytecodeLocs() {
                        return data;
                    }
                });
            }

            //行号和字节码对应关系
            for (BytecodeDumpConsumer.Item item : result) {
                for (Map.Entry<Integer, Integer> entry : item.getBytecodeLocs().entrySet()) {
                    System.out.println("Line " + entry.getValue() + "\t: " + entry.getKey() );
                }
            }
        }
    }


}
