package com.example.util;

import dev.langchain4j.service.V;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 *     Redis工具类，用来存储用户登陆的Token
 * </p>
 * @author vlsmb
 */
@Component
@RequiredArgsConstructor
public class TokenRedisUtil {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-expire}")
    private int refreshExpire;

    /**
     * 向redis中保存用户当前登陆token
     * @param userId 用户ID
     * @param token 权限Token
     */
    public void addToken(Long userId, String token) {
        try {
            ValueOperations<String, String> operations = redisTemplate.opsForValue();
            operations.set(userId2TokenKey(userId), token, refreshExpire, TimeUnit.DAYS);
        } catch (Exception e) {
            throw new RuntimeException("保存Token到Redis失败", e);
        }
    }

    /**
     * 删除某用户的token
     * @param userId 用户ID
     */
    public void removeToken(Long userId) {
        redisTemplate.delete(userId2TokenKey(userId));
    }

    /**
     * 获得某用户AccessToken
     * @param userId 用户ID
     * @return accessToken
     */
    public String getToken(Long userId) {
        ValueOperations<String, String> operations = redisTemplate.opsForValue();
        return operations.get(userId2TokenKey(userId));
    }

    /**
     * 用户ID转为redis token键名
     * @param userId 用户ID
     * @return redisKey名
     */
    private String userId2TokenKey(Long userId) {
        return "token:" + userId;
    }
}
