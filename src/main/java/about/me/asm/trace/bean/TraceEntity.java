package about.me.asm.trace.bean;

import about.me.asm.trace.view.TreeView;
import about.me.utils.ThreadUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TraceEntity {

    protected TreeView view;

    protected int deep;

    public TraceEntity() {
        this.view = createTreeView();
        this.deep = 0;
    }

    private TreeView createTreeView() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");
        String threadTitle = "time=" + dtf.format(LocalDateTime.now()) + ";" + ThreadUtils.getThreadTitle(Thread.currentThread());
        return new TreeView(true, threadTitle);
    }

}
