package about.me.tracer.core;

import about.me.tracer.utils.DateUtils;
import about.me.tracer.utils.ThreadUtils;
import about.me.tracer.view.TreeView;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

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
