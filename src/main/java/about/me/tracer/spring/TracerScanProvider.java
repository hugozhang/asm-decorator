package about.me.tracer.spring;

import about.me.tracer.asm.TracerEnhance;
import about.me.tracer.test.TimerTest;
import about.me.tracer.utils.PkgUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

public class TracerScanProvider {

    private String CONFIG_LOCATION_DELIMITERS = ",; \t\n";

    private String basePackages;


    public TracerScanProvider() {

    }

    public void init() {
        if (this.basePackages == null) {
            throw new IllegalArgumentException("basePackages is required.");
        }
        doScan(StringUtils.tokenizeToStringArray(basePackages, CONFIG_LOCATION_DELIMITERS));
    }

    public void setBasePackages(String basePackages) {
        this.basePackages = basePackages;
    }

    public void doScan(String... basePackages) {
        Set<String> clzFromPkgSet = new HashSet<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for ( String basePackage : basePackages ) {
            Set<String> clzFromPkg = PkgUtils.getClzFromPkg(basePackage);
            clzFromPkgSet.addAll(clzFromPkg);
        }
        for ( String clz : clzFromPkgSet ) {
            TracerEnhance.inject(clz,loader);
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        TracerScanProvider tracerScanProvider = new TracerScanProvider();
        tracerScanProvider.setBasePackages("about.me.tracer.test");
        tracerScanProvider.init();
        Object o = Thread.currentThread().getContextClassLoader().loadClass("about.me.tracer.test.TimerTest").newInstance();
        o.getClass().getMethod("n").invoke(o,new Object[]{});
        new TimerTest().n();
    }
}
