package cn.chenw.redis_lock_demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author chenw
 * @date 2020/4/20 9:54
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class RedisController {
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    /**
     * 初始化库存
     *
     * @return
     */
    @RequestMapping("/initStock")
    String initStock() {
        stringRedisTemplate.opsForValue().set("stock", "20");
        return "success";
    }

    /**
     * 单体架构，通过加锁(synchronized)的方式来防止超卖问题
     * <p>
     * 库存扣减
     *
     * @return
     */
    @RequestMapping("/v1/deductStock")
    String deductStockV1() {
        String key = "stock";
        String result = null;
        synchronized (this) {
            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get(key));
            if (stock > 0) {
                stringRedisTemplate.opsForValue().set(key, String.valueOf(stock - 1));
                log.info("当前线程{}，扣减成功：{}", Thread.currentThread().getName(), (stock - 1));
                result = "扣减成功：" + (stock - 1);
            } else {
                log.info("当前线程{}，库存不足", Thread.currentThread().getName());
                result = "库存不足";
            }
            return result;
        }
    }

    /**
     * 分布式架构 - 1
     * 解决集群环境下使用synchronized锁不能保证原子性的问题
     * 问题点：存在当前锁被上一个线程释放的问题
     *
     * <p>
     * <p>
     * 库存扣减
     *
     * @return
     */
    @RequestMapping("/v2/deductStock")
    String deductStockV2() {
        String lockKey = "lockKey";
        String key = "stock";
        String result = null;
        try {
            //判断锁是否存在，如果不存在则新加一个锁，并设置超时时间，防止死锁情况
            Boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockKey, 10, TimeUnit.SECONDS);
            if (!isLock) {
                return "error";
            }
            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get(key));
            if (stock > 0) {
                stringRedisTemplate.opsForValue().set(key, String.valueOf(stock - 1));
                log.info("扣减成功：{}", (stock - 1));
                result = "扣减成功：" + (stock - 1);
            } else {
                log.info("库存不足");
                result = "库存不足";
            }
        } finally {
            //释放锁
            stringRedisTemplate.delete(lockKey);
        }
        return result;
    }

    /**
     * 分布式架构 - 2
     * 解决当前锁被上一个线程释放的问题
     * 问题点：锁超时，业务还未执行完的情况
     *
     * <p>
     * <p>
     * 库存扣减
     *
     * @return
     */
    @RequestMapping("/v3/deductStock")
    String deductStockV3() {
        String lockKey = "lockKey";
        String key = "stock";
        String result = null;
        String lockId = UUID.randomUUID().toString();
        try {
            //判断锁是否存在，如果不存在则新加一个锁，并设置超时时间，防止死锁情况
            Boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockId, 10, TimeUnit.SECONDS);
            if (!isLock) {
                return "error";
            }
            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get(key));
            if (stock > 0) {
                stringRedisTemplate.opsForValue().set(key, String.valueOf(stock - 1));
                log.info("扣减成功：{}", (stock - 1));
                result = "扣减成功：" + (stock - 1);
            } else {
                log.info("库存不足");
                result = "库存不足";
            }
        } finally {
            //判断该锁是否属于当前线程
            if(lockId.equals(stringRedisTemplate.opsForValue().get(lockKey))){
                //释放锁
                stringRedisTemplate.delete(lockKey);
            }
        }
        return result;
    }

    @Autowired
    Redisson redisson;

    /**
     * 分布式架构 - 3
     * 利用redission 实现分布式锁
     *
     * <p>
     * <p>
     * 库存扣减
     *
     * @return
     */
    @RequestMapping("/v4/deductStock")
    String deductStockV4() {
        String lockKey = "lockKey";
        String key = "stock";
        String result = null;
        //获取锁对象
        RLock redissonLock = redisson.getLock(lockKey);
        boolean isLock;
        try {
            //尝试枷锁，最多等待3秒，加锁以后4秒自动解锁
           // isLock = redissonLock.tryLock(3,4, TimeUnit.SECONDS);
            //log.info("获取锁的结果：{}",isLock);
            redissonLock.lock(4,TimeUnit.SECONDS);
            //if(isLock){
                int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get(key));

                if (stock > 0) {
                    //模拟业务用时
                    Thread.sleep(5000);
                    stringRedisTemplate.opsForValue().set(key, String.valueOf(stock - 1));
                    log.info("扣减成功：{}", (stock - 1));
                    result = "扣减成功：" + (stock - 1);
                } else {
                    log.info("库存不足");
                    result = "库存不足";
                }
            /*}else {
                log.info("获取锁失败");
                return "获取锁失败";
            }*/
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("异常{}",e);
        } finally {
            //释放锁
            //判断要解锁的key是否被锁定并且是否被当前线程持有，满足时才解锁
            if(redissonLock.isLocked()){
                if(redissonLock.isHeldByCurrentThread()){
                    redissonLock.unlock();
                }
            }
        }
        log.info("------------------------");
        return result;
    }

    //https://www.whatyun.cn/article/294
}
