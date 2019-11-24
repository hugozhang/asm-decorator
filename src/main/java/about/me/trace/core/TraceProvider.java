package about.me.trace.core;

import about.me.trace.asm.TraceEnhance;
import about.me.trace.test.bean.TimerTest;
import about.me.trace.test.User;
import about.me.utils.PkgUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class TraceProvider {

    private static AntPathMatcher antPathMatcher = new AntPathMatcher();

    private static String locationSuffix = "/**/*.class";

    public void scan(String locationPattern) {
        String rootDirPath = determineRootDir(toLocation(locationPattern));
//        String subPattern = locationPattern.substring(rootDirPath.length());
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Set<String> clzFromPkg = PkgUtils.getClzFromPkg(rootDirPath.replace('/','.'));
        for ( String clz : clzFromPkg ) {
            if (antPathMatcher.match(toLocation(locationPattern + locationSuffix),toLocation(clz + locationSuffix))) {
                log.info("Match class is {}.",clz);
                TraceEnhance.inject(clz,loader);
            }
        }
    }

    private String toLocation(String packagePath) {
        String replace = packagePath.replace('.', '/');
        return replace;
    }

    private String determineRootDir(String location) {
        int prefixEnd = location.indexOf(":") + 1;
        int rootDirEnd = location.length();
        while (rootDirEnd > prefixEnd && antPathMatcher.isPattern(location.substring(prefixEnd, rootDirEnd))) {
            rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2);
        }
        if (rootDirEnd == 0) {
            rootDirEnd = prefixEnd;
        }
        return location.substring(0, rootDirEnd);
    }

    public TraceProvider() {
//        if (basePackages == null) {
//            throw new IllegalArgumentException("basePackages is required.");
//        }
//        doScan(StringUtils.tokenizeToStringArray(basePackages, CONFIG_LOCATION_DELIMITERS));
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

    public static void main(String[] args) {
        new TraceProvider().scan("about.me.trace.test.bean");
        User user = new User();
        user.setName("Java");
        user.setA(123456789);
        new TimerTest().get(user);

//        System.out.println(isMatch("com/juma/*/a","com/juma/b/a"));

//        new TraceProvider().scan("about/me/**/bean");


    }
}
