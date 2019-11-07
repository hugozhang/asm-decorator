package about.me.trace.core;

import about.me.trace.asm.TraceEnhance;
import about.me.trace.utils.PkgUtils;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

public class TraceProvider implements Ordered {

    private String CONFIG_LOCATION_DELIMITERS = ",; \t\n";

    public TraceProvider(String basePackages) {
        if (basePackages == null) {
            throw new IllegalArgumentException("basePackages is required.");
        }
        doScan(StringUtils.tokenizeToStringArray(basePackages, CONFIG_LOCATION_DELIMITERS));
    }

    public void doScan(String... basePackages) {
        Set<String> clzFromPkgSet = new HashSet<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for ( String basePackage : basePackages ) {
            Set<String> clzFromPkg = PkgUtils.getClzFromPkg(basePackage);
            clzFromPkgSet.addAll(clzFromPkg);
        }
        for ( String clz : clzFromPkgSet ) {
            TraceEnhance.inject(clz,loader);
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
//        TraceProvider tracerScanProvider = new TraceProvider();
//        tracerScanProvider.setBasePackages("about.me.trace.test");
//        tracerScanProvider.init();
//        Object o = Thread.currentThread().getContextClassLoader().loadClass("about.me.trace.test.TimerTest").newInstance();
//        o.getClass().getMethod("n").invoke(o,new Object[]{});
//        new TimerTest().n();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
