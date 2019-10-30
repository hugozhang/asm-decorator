package about.me.tracer.core;

import about.me.tracer.view.View;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThreadLocalTracer {

    protected static final ThreadLocal<TraceEntity> threadBoundEntity = new ThreadLocal<TraceEntity>() {
        @Override
        protected TraceEntity initialValue() {
            return new TraceEntity();
        }
    };

    private ThreadLocalTracer() {}

    private static class SingleHolder {
        private static ThreadLocalTracer INSTANCE = new ThreadLocalTracer();
    }

    public static ThreadLocalTracer getInstance(){
        return SingleHolder.INSTANCE;
    }

    public static View getView() {
        return ThreadLocalTracer.getInstance().threadBoundEntity.get().view;
    }

    public static void reset() {
        ThreadLocalTracer.getInstance().threadBoundEntity.remove();
    }

    public static void finish() {
        if (--threadBoundEntity.get().deep == 0) {
            log.info(ThreadLocalTracer.getView().draw());
            System.out.println(ThreadLocalTracer.getView().draw());
            ThreadLocalTracer.reset();
        }
    }

    public static void main(String[] args) {
        System.out.println(ThreadLocalTracer.getView().begin("com.juma.tgm.a"));

        System.out.println(ThreadLocalTracer.getView().begin("com.juma.tgm.b"));
        System.out.println(ThreadLocalTracer.getView().begin("com.juma.tgm.c"));
        System.out.println(ThreadLocalTracer.getView().end());
        System.out.println(ThreadLocalTracer.getView().end());
        System.out.println(ThreadLocalTracer.getView().begin("com.juma.tgm.b"));
        System.out.println(ThreadLocalTracer.getView().begin("com.juma.tgm.c"));
        System.out.println(ThreadLocalTracer.getView().end());
        System.out.println(ThreadLocalTracer.getView().end());
        System.out.println(ThreadLocalTracer.getView().begin("com.juma.tgm.b"));
        System.out.println(ThreadLocalTracer.getView().begin("com.juma.tgm.c"));
        System.out.println(ThreadLocalTracer.getView().end());
        System.out.println(ThreadLocalTracer.getView().end());
        System.out.println(ThreadLocalTracer.getView().end());
        System.out.println(ThreadLocalTracer.getView().draw());
        ThreadLocalTracer.reset();
        System.out.println(ThreadLocalTracer.getView().draw());

        ThreadLocalTracer.reset();


    }

}
