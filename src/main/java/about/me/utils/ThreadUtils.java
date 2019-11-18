package about.me.utils;

public class ThreadUtils {

    public static String getThreadTitle(Thread currentThread) {
        StringBuilder sb = new StringBuilder("thread_name=");
        sb.append(currentThread.getName())
                .append(";id=").append(Long.toHexString(currentThread.getId()))
                .append(";is_daemon=").append(currentThread.isDaemon())
                .append(";priority=").append(currentThread.getPriority())
                .append(";TCCL=").append(getTCCL(currentThread));
        return sb.toString();
    }

    private static String getTCCL(Thread currentThread) {
        if (null == currentThread.getContextClassLoader()) {
            return "null";
        }
        return currentThread.getContextClassLoader().getClass().getName() + "@" + Integer.toHexString(currentThread.getContextClassLoader().hashCode());
    }

}
