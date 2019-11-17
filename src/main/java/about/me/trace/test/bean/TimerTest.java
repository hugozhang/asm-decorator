package about.me.trace.test.bean;


import about.me.cache.annotation.Cache;
import about.me.cache.redis.HessianRedisTemplate;
import about.me.trace.core.Trace;

import java.util.concurrent.TimeUnit;


public class TimerTest {

    @Cache(group = "aa",key = "bb",expire = 1,timeUnit = TimeUnit.DAYS)
    public Object get(String a)  {
        if (a != null) {
            throw new RuntimeException("123");
        }
        return new User();
//
//        try {
//                User a = new User();
//                if (a != null) {
////                    throw new RuntimeException("qq");
////                    return  aa();
//                    return a;
//                }
//
////            try {
////                System.out.println(12);
////                String a = "123";
////                if (a == null) {
////                    return new User();
////                }
////                System.out.println(34);
////                return new User();
////            } catch (Exception e) {
////                throw new RuntimeException("");
////            }
//            return new User();
//        } catch (Exception e) {
//            throw e;
//        }
//        return new User();




//        try {
//
//        } catch (Exception e) {
//            throw e;
//        }


//        return new User();
    }

//    public Object aa(){
//        Object object = HessianRedisTemplate.getObject("", "");
//        if (object != null) return object;
//        return new User();
//    }

//    public Object geta() {
//        try {
//            Trace.enter("a.b");
////            Object object = HessianRedisTemplate.getObject("", "");
////            if (object != null) return object;
//            Object o = get();
//            Trace.exit();
////            HessianRedisTemplate.putObject("","",o,1,TimeUnit.MINUTES);
//            return o;
//        } catch (Exception e) {
//            Trace.exit(e.getMessage());
//            throw e;
//        }
//    }
}
