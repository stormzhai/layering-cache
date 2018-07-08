package com.xiaolyuh.listener;

import com.alibaba.fastjson.JSON;
import com.xiaolyuh.cache.Cache;
import com.xiaolyuh.cache.LayeringCache;
import com.xiaolyuh.manager.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * redis消息的订阅者
 *
 * @author yuhao.wang
 */
public class RedisMessageListener extends MessageListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(RedisPublisher.class);

    /**
     * 缓存管理器
     */
    private CacheManager cacheManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        super.onMessage(message, pattern);
        log.info("redis消息订阅者接收到频道【{}】发布的消息。消息内容：{}", new String(message.getChannel()), message.toString().getBytes());
        // 解析订阅发布的信息，获取缓存的名称和缓存的key
        String ms = new String(message.getBody());
        @SuppressWarnings("unchecked")
        RedisPubSubMessage redisPubSubMessage = JSON.parseObject(ms, RedisPubSubMessage.class);

        // 根据缓存名称获取多级缓存
        Cache cache = cacheManager.getCache(redisPubSubMessage.getCacheName());

        // 判断缓存是否是多级缓存
        if (cache != null && cache instanceof LayeringCache) {
            switch (redisPubSubMessage.getMessageType()) {
                case EVICT:
                    // 获取一级缓存，并删除一级缓存数据
                    ((LayeringCache) cache).getFirstCache().evict(redisPubSubMessage.getKey());
                    log.info("删除一级缓存{}数据,key:{}", redisPubSubMessage.getCacheName(), redisPubSubMessage.getKey());
                    break;

                case CLEAR:
                    // 获取一级缓存，并删除一级缓存数据
                    ((LayeringCache) cache).getFirstCache().clear();
                    log.info("清除一级缓存{}数据", redisPubSubMessage.getCacheName());
                    break;

                default:
                    log.error("接收到没有定义的订阅消息频道数据");
                    break;
            }

        }
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
}
