package cn.chenw.redis_lock_demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * @author chenw
 * @date 2020/4/22 9:34
 */
@Slf4j
@RestController
@RequestMapping("/api2")
public class RedissonController {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    Redisson redisson;

    @GetMapping("/v1/deductStock")
    String deductStockv1() {
        String result;
        //库存key
        String stock = "stock";
        //锁key
        String lockKey = "lockKey";
        //获取锁对象
        RLock redissonLock = redisson.getLock(lockKey);

        try {
            //加锁，设置锁的有效期，会使看门狗失效
            //redissonLock.lock(30, TimeUnit.SECONDS);
            //加锁，默认30s有效期，默认设置不会使看门狗失效
            redissonLock.lock();

            int number = Integer.parseInt(stringRedisTemplate.opsForValue().get(stock));
            if (number > 0) {
                Thread.sleep(35000);
                stringRedisTemplate.opsForValue().set(stock, String.valueOf((number - 1)));
                log.info("扣减成功：{}", (number - 1));
                result = "下单成功";
            } else {
                result = "没有库存";
            }
            return result;
        } catch (InterruptedException e) {
            log.error("异常{}", e);
            return "服务异常";
        } finally {
            //释放锁
            //判断要解锁的key是否被锁定并且是否被当前线程持有，满足时才解锁
            if (redissonLock.isLocked()) {
                if (redissonLock.isHeldByCurrentThread()) {
                    redissonLock.unlock();
                }
            }
        }
    }

    @GetMapping("/v2/deductStock")
    String deductStockv2() {
        String result;
        //库存key
        String stock = "stock";
        //锁key
        String lockKey = "lockKey";
        //获取锁对象
        RLock redissonLock = redisson.getLock(lockKey);

        try {
            //尝试获取锁，默认锁的有效时间为30s
            boolean isGetLock = redissonLock.tryLock();
            if (isGetLock){

                int number = Integer.parseInt(stringRedisTemplate.opsForValue().get(stock));
                if (number > 0) {
                    Thread.sleep(35000);
                    stringRedisTemplate.opsForValue().set(stock, String.valueOf((number - 1)));
                    log.info("扣减成功：{}", (number - 1));
                    result = "下单成功";
                } else {
                    result = "没有库存";
                }
            }else {
                result ="锁被占用";
                log.info(result);
            }
            return result;
        } catch (InterruptedException e) {
            log.error("异常{}", e);
            return "服务异常";
        } finally {
            //释放锁
            //判断要解锁的key是否被锁定并且是否被当前线程持有，满足时才解锁
            if (redissonLock.isLocked()) {
                if (redissonLock.isHeldByCurrentThread()) {
                    redissonLock.unlock();
                }
            }
        }
    }

}
