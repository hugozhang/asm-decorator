package about.me.trace.core;

import about.me.trace.asm.TraceEnhance;
import about.me.trace.test.bean.TimerTest;
import about.me.trace.utils.PkgUtils;
import org.springframework.core.Ordered;
import org.springframework.util.AntPathMatcher;

import java.util.HashSet;
import java.util.Set;

public class TraceProvider implements Ordered {


    public static AntPathMatcher pathMatcher = new AntPathMatcher();

    public TraceProvider(String basePackage) {
        if (basePackage == null) {
            throw new IllegalArgumentException("basePackage is required.");
        }
        String locationPattern = basePackage + "/**/*.class";
        String rootDirPath = determineRootDir(basePackage);
        String subPattern = locationPattern.substring(rootDirPath.length());
        doScan(subPattern,rootDirPath);
    }

    protected String determineRootDir(String location) {
        int prefixEnd = location.indexOf(":") + 1;
        int rootDirEnd = location.length();
        while (rootDirEnd > prefixEnd && pathMatcher.isPattern(location.substring(prefixEnd, rootDirEnd))) {
            rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2) + 1;
        }
        if (rootDirEnd == 0) {
            rootDirEnd = prefixEnd;
        }
        return location.substring(0, rootDirEnd -1);
    }

    public void doScan(String subPattern,String rootDirPath) {
        Set<String> clzFromPkgSet = new HashSet<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Set<String> clzFromPkg = PkgUtils.getClzFromPkg(rootDirPath);
        clzFromPkgSet.addAll(clzFromPkg);
        for ( String clz : clzFromPkgSet ) {
            if (!isMatch(rootDirPath + subPattern,clz)) continue;
            TraceEnhance.inject(clz,loader);
        }
    }

    public boolean isMatch(String subPattern,String clz) {
        return pathMatcher.match(subPattern, clz);
    }


//    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
//        new TraceProvider("about.me.trace.test");
//        new TimerTest().get();
//    }

    public static void main(String[] args) {

        new TraceProvider("about/me/**/test/bean");

        new TimerTest().get();

        test("com*.test", "comaaaa.test");  // true
        test("com*/test", "com/test");      // true
        test("com**/test", "comaaaa/test"); // true
        test("com**/test", "com/test");     // true
        test("com**/test", "com/a/test");   // false

        test("com/*/test", "com/test");     // false
        test("com/*/test", "com/a/test");   // true
        test("com/*/test", "com/a/b/test"); // false

        test("com/**/test", "com/test");    // true
        test("com/**/test", "com/a/test");  // true
        test("com/**/test", "com/a/b/test");// true
    }

    public static void test(String pattern, String text) {
        System.out.println(String.format("%s => %s : %s", pattern, text, pathMatcher.match(pattern, text)));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
