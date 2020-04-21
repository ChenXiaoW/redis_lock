package cn.chenw.redis_lock_demo.config;

import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author  chenw
 * @date  2020/4/20 15:34
 */
@Configuration
public class RedissonConfig {

    @Bean
    public Redisson redisson(){
        //单机模式
        Config config = new Config();
        config.useSingleServer().setAddress("redis://47.106.220.141:6739")
                .setDatabase(0);
        return (Redisson) Redisson.create(config);
    }
}
