package about.me.trace.core;

import about.me.trace.view.TreeView;
import about.me.utils.DateUtils;
import about.me.utils.ThreadUtils;
import lombok.Data;

@Data
public class TraceEntity {

    protected TreeView view;

    protected int deep;

    public TraceEntity() {
        this.view = createTreeView();
        this.deep = 0;
    }

    private TreeView createTreeView() {
        String threadTitle = "time=" + DateUtils.getCurrentTime()+ ";" + ThreadUtils.getThreadTitle(Thread.currentThread());
        return new TreeView(true, threadTitle);
    }

}
