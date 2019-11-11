package about.me.cache.redis;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.concurrent.TimeUnit;

public class HessianRedisTemplate extends RedisTemplate<String, Object> implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    public HessianRedisTemplate() {
        RedisSerializer<Object> hessianSerializer = new HessianRedisSerializer();
        setKeySerializer(hessianSerializer);
        setValueSerializer(hessianSerializer);
        setHashKeySerializer(hessianSerializer);
        setHashValueSerializer(hessianSerializer);
    }

    private Object get(String group, String key) {
        return boundHashOps(group).get(key);
    }

    private void put(String group,String key,Object value,long expire, TimeUnit timeUnit) {
        boundHashOps(group).put(key,value);
        boundHashOps(group).expire(expire,timeUnit);
    }

    private static HessianRedisTemplate getInstance() {
        if (applicationContext == null) {
            throw new BeanInstantiationException(HessianRedisTemplate.class,"HessianRedisTemplate not exist in ApplicationContext.");
        }
        HessianRedisTemplate redisTemplate = applicationContext.getBean(HessianRedisTemplate.class);
        return redisTemplate;
    }

    public static Object getObject(String group, String key) {
        return getInstance().get(group,key);
    }

    public static void putObject(String group,String key,Object value,long expire, TimeUnit timeUnit) {
        getInstance().put(group,key,value,expire,timeUnit);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
