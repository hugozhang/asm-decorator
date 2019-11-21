package about.me.trace.core;

import about.me.trace.view.View;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThreadLocalTrace {

    protected static final ThreadLocal<TraceEntity> threadBoundEntity = new ThreadLocal<TraceEntity>() {
        @Override
        protected TraceEntity initialValue() {
            return new TraceEntity();
        }
    };

    private ThreadLocalTrace() {}

    private static class SingleHolder {
        private static ThreadLocalTrace INSTANCE = new ThreadLocalTrace();
    }

    public static ThreadLocalTrace getInstance(){
        return SingleHolder.INSTANCE;
    }

    public static View getView() {
        return ThreadLocalTrace.getInstance().threadBoundEntity.get().view;
    }

    public static void reset() {
        ThreadLocalTrace.getInstance().threadBoundEntity.remove();
    }


    public static void finish() {
        if (--threadBoundEntity.get().deep == 0) {
            log.info(ThreadLocalTrace.getView().draw());
            ThreadLocalTrace.reset();
        }
    }

    public static void main(String[] args) {
        System.out.println(ThreadLocalTrace.getView().begin("com.juma.tgm.a"));

        System.out.println(ThreadLocalTrace.getView().begin("com.juma.tgm.b"));
        System.out.println(ThreadLocalTrace.getView().begin("com.juma.tgm.c"));
        System.out.println(ThreadLocalTrace.getView().end());
        System.out.println(ThreadLocalTrace.getView().end());
        System.out.println(ThreadLocalTrace.getView().begin("com.juma.tgm.b"));
        System.out.println(ThreadLocalTrace.getView().begin("com.juma.tgm.c"));
        System.out.println(ThreadLocalTrace.getView().end());
        System.out.println(ThreadLocalTrace.getView().end());
        System.out.println(ThreadLocalTrace.getView().begin("com.juma.tgm.b"));
        System.out.println(ThreadLocalTrace.getView().begin("com.juma.tgm.c"));
        System.out.println(ThreadLocalTrace.getView().end());
        System.out.println(ThreadLocalTrace.getView().end());
        System.out.println(ThreadLocalTrace.getView().end());
        System.out.println(ThreadLocalTrace.getView().draw());
        ThreadLocalTrace.reset();
        System.out.println(ThreadLocalTrace.getView().draw());

        ThreadLocalTrace.reset();


    }

}
