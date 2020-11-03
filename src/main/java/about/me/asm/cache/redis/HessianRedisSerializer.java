package about.me.asm.cache.redis;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;
import com.alibaba.com.caucho.hessian.io.Hessian2Output;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public class HessianRedisSerializer implements RedisSerializer<Object> {

    @Override
    public byte[] serialize(Object o) throws SerializationException {
        if (o == null) return null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Hessian2Output hessian2Output = new Hessian2Output(byteArrayOutputStream);
        try {
            hessian2Output.writeObject(o);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            log.error(e.getMessage(),e);
        } finally {
            try {
                if (hessian2Output != null) {
                    hessian2Output.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(),e);
            }
        }
        return null;
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null) return null;
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        Hessian2Input hessian2Input = new Hessian2Input(byteArrayInputStream);
        try {
            return hessian2Input.readObject();
        } catch (IOException e) {
            log.error(e.getMessage(),e);
        } finally {
            if (hessian2Input != null) {
                try {
                    hessian2Input.close();
                } catch (IOException e) {
                    log.error(e.getMessage(),e);
                }
            }
        }
        return null;
    }

}
