package test;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author 胡帅博
 * @date 2022/4/16 10:15
 */
public class CFRTest {
    public static void main(String[] args) {
        OptionsImpl options = new OptionsImpl(new HashMap<>(0));
        List<String>  files = new ArrayList<>();

        files.add("G:\\kaifa_environment\\code\\java\\java-agent\\target\\classes\\App.class");
      //  files.add("java.lang.Object");
        CfrDriver cfrDriver = new CfrDriver.Builder().withBuiltOptions(options).build();
        cfrDriver.analyse(files);
        //cfrDriver.analyse(files);
    }
}
