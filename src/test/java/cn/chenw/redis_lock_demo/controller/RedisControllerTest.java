package cn.chenw.redis_lock_demo.controller;



import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author chenw
 * @title: RedisControllerTest
 * @description: TODO
 * @date 2020/4/20 9:55
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisControllerTest {

    @Autowired
    StringRedisTemplate stringRedisTemplate;


    @Test
    public void initStock() {
        stringRedisTemplate.opsForValue().set("stock","20");
    }
}