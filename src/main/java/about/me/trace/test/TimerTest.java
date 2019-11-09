package about.me.trace.test;


import about.me.cache.annotation.Cache;

import java.util.concurrent.TimeUnit;


public class TimerTest {

    @Cache(group = "aa",key = "bb",expire = 1,timeUnit = TimeUnit.DAYS)
    public Object get() {
        return new User();
    }


    public String geta(){
        return "";
    }
}
