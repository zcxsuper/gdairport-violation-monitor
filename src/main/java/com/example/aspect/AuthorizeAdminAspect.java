package com.example.aspect;

import com.example.enums.UserRoleEnum;
import com.example.exception.UnauthorizedException;
import com.example.mapper.UserMapper;
import com.example.util.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizeAdminAspect {

    private final UserMapper userMapper;

    @Pointcut("@annotation(com.example.annotation.AuthorizeAdmin)")
    public void pointcut() {
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 输入参数
        // Object[] args = joinPoint.getArgs();
        Long userId = UserContextUtil.getUserId();
        if (!userMapper.selectById(userId).getAuth().equals(UserRoleEnum.ADMIN)) {
            throw new UnauthorizedException("权限不足");
        }

        return joinPoint.proceed();
    }
}

