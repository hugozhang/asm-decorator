package about.me.asm.trace.view;

public interface View {

    String draw();

    View begin(String message);

    View end();

    View end(String mark);

}
