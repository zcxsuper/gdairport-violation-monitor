package com.example.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.exception.TokenInvalidException;
import com.example.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.util.validation.metadata.DatabaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;


/**
 * <p>
 * jwt工具类
 * </p>
 *
 * @author vlsmb
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.security}")
    private String security;
    @Value("${jwt.access-expire}")
    private int accessExpire;

    private final TokenRedisUtil tokenRedisUtil;
    /**
     * 生成JWT令牌
     *
     * @param userId 用户信息
     * @return JWT令牌字符串
     */
    public String generateToken(Long userId) {
        return JWT.create()
                .withClaim("userId", userId)
                .withExpiresAt(expireTime(accessExpire))
                .sign(Algorithm.HMAC256(security));
    }

    /**
     * 获得JWT令牌信息
     *
     * @param token JWT令牌
     * @return UserClaims对象
     */
//    public Long verifyToken(String token) {
//        try {
//            return JWT.require(Algorithm.HMAC256(security))
//                    .build()
//                    .verify(token)
//                    .getClaim("userId")
//                    .asLong();
//        } catch (Exception e) {
//            throw new TokenInvalidException("Token 无效或者已经过期", e);
//        }
//    }

    /**
     * 计算令牌过期时间
     *
     * @return Date对象
     */
    private Date expireTime(int days) {
        return new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * days);
    }

    /**
     * 无感刷新 Token
     */
    public Long verifyToken(String token, HttpServletResponse response) {
        try {
            // 正常校验（未过期、签名正确）
            return JWT.require(Algorithm.HMAC256(security))
                    .build()
                    .verify(token)
                    .getClaim("userId")
                    .asLong();
        } catch (TokenExpiredException e) {
            // accessToken过期 获取refreshToken并刷新
            DecodedJWT decodeToken = JWT.decode(token);
            Long userId = decodeToken.getClaim("userId").asLong();
            if (tokenRedisUtil.getToken(userId) == null) {
                throw new UnauthorizedException("token已过期");
            }
            String newToken = generateToken(userId);
            if (newToken == null) {
                throw new DatabaseException("保存Token到Redis失败", e);
            }
            tokenRedisUtil.addToken(userId, newToken);
            // 写入新的 token 到响应头
            response.setHeader("Authorization", newToken);
            return userId;
        } catch (JWTVerificationException e) {
            // 捕获其他所有验证异常，例如签名无效
            throw new UnauthorizedException("token无效");
        }
    }
}
