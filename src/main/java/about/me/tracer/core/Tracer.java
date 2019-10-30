package about.me.tracer.core;

public class Tracer {

    public static void enter(String message) {
        ThreadLocalTracer.getView().begin(message);
        ThreadLocalTracer.getInstance().threadBoundEntity.get().deep++;
    }

    public static void exit() {
        ThreadLocalTracer.getView().end();
        ThreadLocalTracer.finish();
    }

    public static void exit(String message) {
        ThreadLocalTracer.getView().end(message);
        ThreadLocalTracer.finish();
    }

    public static void main(String[] args) {
        Tracer.enter("com.juma.a");
        Tracer.exit();
        Tracer.exit("throw exception");
    }
}
