package about.me.trace.test.bean;


import about.me.cache.annotation.Cache;
import about.me.cache.redis.HessianRedisTemplate;
import about.me.trace.core.Trace;

import java.util.concurrent.TimeUnit;


public class TimerTest {

    @Cache(group = "aa",key = "bb",expire = 1,timeUnit = TimeUnit.DAYS)
    public Object get(String a)  {

        User user = new User();
        user.setA(22);
        return user;
    }

//    @Cache(group = "aa",key = "bb",expire = 1,timeUnit = TimeUnit.DAYS)
//    public Object get(String a)  {
//        Object o = HessianRedisTemplate.getObject("", "");
//        if (o != null) return o;
//        if (a != null) {
//            throw new RuntimeException("123");
//        }
//        User user = new User();
//        HessianRedisTemplate.putObject("","",user,1,TimeUnit.MINUTES);
//        return user;
//    }


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
