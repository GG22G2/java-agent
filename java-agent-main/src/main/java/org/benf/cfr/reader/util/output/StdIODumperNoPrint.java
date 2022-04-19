package org.benf.cfr.reader.util.output;

import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.getopt.Options;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;


/**
 * 修改访问权限
 */
public class StdIODumperNoPrint extends StdIODumper {
    int line = 1;
    StringBuilder lineCode = new StringBuilder(500);
    Map<Integer, String> codes = new HashMap<>();

    public StdIODumperNoPrint(TypeUsageInformation typeUsageInformation, Options options, IllegalIdentifierDump illegalIdentifierDump, MovableDumperContext context) {
        super(typeUsageInformation, options, illegalIdentifierDump, context);
    }

    @Override
    protected void write(String s) {
        if (s.equals("\n")) {
            String s1 = lineCode.toString();
            codes.put(line, s1);
            lineCode.delete(0, lineCode.length());
            line++;
        } else {
            lineCode.append(s);
        }
    }

    public Map<Integer, String> getCodes() {
        return codes;
    }

    public void flush() {
        if (lineCode.length() > 0) {
            String s1 = lineCode.toString();
            codes.put(line, s1);
            lineCode.delete(0, lineCode.length());
        }
    }

    @Override
    public void close() {
        flush();

        codes.forEach(new BiConsumer<Integer, String>() {
            @Override
            public void accept(Integer integer, String s) {
                System.out.println(integer + ":   " + s );
            }
        });
    }
}
