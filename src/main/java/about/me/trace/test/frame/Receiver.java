package about.me.trace.test.frame;

import about.me.trace.test.User;


public class Receiver {

    public Object do1() {
        try {
            String a = "";
            if (a == null) {
                return new User();
            }
            System.out.println("do1");
            return new User();
        } catch (Exception e) {
            throw new RuntimeException("");
        }
//        try {
//            Trace.enter("about.me.trace.test.frame.Receiver.do1");
//            Object object = HessianRedisTemplate.getObject("", "");
//            if (object != null) {
//                return object;
//            }
//            HessianRedisTemplate.putObject("","","",10, TimeUnit.MINUTES);
//            return new User();
//        } catch (Exception e) {
//            Trace.exit(e.getMessage());
//            throw e;
//        }
    }

    public  void a(){
    }

}
