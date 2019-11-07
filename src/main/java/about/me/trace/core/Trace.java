package about.me.trace.core;

public class Trace {

    public static void enter(String message) {
        ThreadLocalTrace.getView().begin(message);
        ThreadLocalTrace.getInstance().threadBoundEntity.get().deep++;
    }

    public static void exit() {
        ThreadLocalTrace.getView().end();
        ThreadLocalTrace.finish();
    }

    public static void exit(String message) {
        ThreadLocalTrace.getView().end(message);
        ThreadLocalTrace.finish();
    }

    public static void main(String[] args) {
        Trace.enter("com.juma.a");
        Trace.exit("throw exception");
        Trace.enter("com.juma.a");
        Trace.exit();
    }
}
