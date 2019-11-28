package about.me.cache.mybatis;

import about.me.cache.redis.HessianRedisTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.Cache;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;

@Slf4j
public class MyBatisRedisCache implements Cache {

    private String id;

    public MyBatisRedisCache(String id) {
        if (id == null) {
            throw new IllegalArgumentException("MyCache instances require an ID");
        }
        this.id = id;
    }

    @Override
    public String toString() {
        return "MyBatisRedisCache{" +
                "id='" + id + '\'' +
                '}';
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void putObject(Object key, Object value) {
        if (value == null) return;
        HessianRedisTemplate.putObject(id,key,value,8,TimeUnit.HOURS);
        log.info("put into cache -> {}.",key);
    }


    @Override
    public Object getObject(Object key) {
        return HessianRedisTemplate.getObject(id,key.toString());
    }

    @Override
    public Object removeObject(Object key) {
        log.info("remove from cache -> {}.",key);
        return HessianRedisTemplate.removeObject(id,key);
    }

    @Override
    public void clear() {
        HessianRedisTemplate.clearGroup(id);
        log.info("clear cache -> {}.",id);
    }

    @Override
    public int getSize() {
        return HessianRedisTemplate.getGroupSize(id);
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return new DummyReadWriteLock();
    }
}
