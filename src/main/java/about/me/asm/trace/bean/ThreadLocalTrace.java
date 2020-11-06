package about.me.asm.trace.bean;

import about.me.asm.trace.view.View;
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
        ThreadLocalTrace.getView().begin("com.juma.tgm.a");

        ThreadLocalTrace.getView().begin("com.juma.tgm.b");
        ThreadLocalTrace.getView().begin("com.juma.tgm.c");
        ThreadLocalTrace.getView().begin("com.juma.tgm.c2");
        ThreadLocalTrace.getView().end();
        ThreadLocalTrace.getView().end();
        ThreadLocalTrace.getView().begin("com.juma.tgm.b");
        ThreadLocalTrace.getView().end();
        ThreadLocalTrace.getView().end();
        ThreadLocalTrace.getView().end();
        ThreadLocalTrace.getView().begin("com.juma.tgm.b");
        ThreadLocalTrace.getView().begin("com.juma.tgm.c");
        ThreadLocalTrace.getView().end();
        ThreadLocalTrace.getView().end();
        ThreadLocalTrace.getView().end();
        ThreadLocalTrace.finish();

        System.out.print(ThreadLocalTrace.getView().draw());

        ThreadLocalTrace.reset();
        ThreadLocalTrace.getView().draw();

        ThreadLocalTrace.reset();


    }

}
