package about.me.tracer.view;

public interface View {

    String draw();

    View begin(String message);

    View end();

    View end(String mark);

}
