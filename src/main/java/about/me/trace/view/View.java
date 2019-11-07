package about.me.trace.view;

public interface View {

    String draw();

    View begin(String message);

    View end();

    View end(String mark);

}
