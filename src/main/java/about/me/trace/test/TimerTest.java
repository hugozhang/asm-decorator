package about.me.trace.test;


import about.me.cache.annotation.Cache;
import about.me.cache.redis.HessianRedisTemplate;
import about.me.trace.core.Trace;

import java.util.concurrent.TimeUnit;


public class TimerTest {

    @Cache(group = "aa",key = "bb",expire = 1,timeUnit = TimeUnit.DAYS)
    public Object get()  {
        return new User();
    }

//    public Object geta() {
//
//        try {
//            Object object = HessianRedisTemplate.getObject("", "");
//            if (object != null) return object;
//            return get();
//        } catch (RuntimeException e) {
//            Trace.exit(e.getMessage());
//            throw e;
//        }
//
//    }
}
