package about.me.asm.test.bean;


import about.me.asm.cache.annotation.MyCache;
import about.me.asm.test.User;

import java.util.concurrent.TimeUnit;


public class TimerTest {

//    @MyCache(group = "aa",key = "bb",expire = 1,timeUnit = TimeUnit.DAYS)
//    public User get(String a)  {
//
//        User user = new User();
//        user.getA();
//        return user;
//    }

//    @MyCacheEvict(group = "aa",key = "u")
    @MyCache(group = "aa",key = "u",expire = 1,timeUnit = TimeUnit.DAYS)
    public User get(String u)  {
//        HessianRedisTemplate.removeObject("","");
//        if (u == null) return new User();
        User user = new User();
        return user;
    }

//    public Object geta(){
//        try{
//            if (true){
//                throwException();
//            }
//            a2();
//        } catch (Exception e) {
//
//        }
//        return new User();
//    }
//
//    public void a1(){}
//
//    public void a2(){}
//
//
//    public void throwException(){
//        throw new RuntimeException("");
//    }

//    @MyCache(group = "aa",key = "bb",expire = 1,timeUnit = TimeUnit.DAYS)
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
