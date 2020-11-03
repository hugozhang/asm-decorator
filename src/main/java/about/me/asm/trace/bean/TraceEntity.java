package about.me.asm.trace.bean;

import about.me.asm.trace.view.TreeView;
import about.me.utils.ThreadUtils;
import lombok.Data;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Data
public class TraceEntity {

    protected TreeView view;

    protected int deep;

    public TraceEntity() {
        this.view = createTreeView();
        this.deep = 0;
    }

    private TreeView createTreeView() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String threadTitle = "time=" + dtf.format(Instant.now()) + ";" + ThreadUtils.getThreadTitle(Thread.currentThread());
        return new TreeView(true, threadTitle);
    }

}
