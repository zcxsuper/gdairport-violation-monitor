package com.example.aspect;

import com.example.config.RabbitConfig;
import com.example.domain.entity.Message;
import com.example.enums.AssistantTypeEnum;
import com.example.enums.MessageTypeEnum;
import com.example.enums.SenderTypeEnum;
import com.example.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static com.example.enums.AssistantTypeEnum.LOCAL;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatFlowAspect {


    private final RabbitTemplate rabbitTemplate;
    private final SessionService sessionService;

    private void sendMessage(Message message) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.MESSAGE_EXCHANGE,
                RabbitConfig.MESSAGE_ROUTING_KEY,
                message
        );
    }

    @Pointcut("@annotation(com.example.annotation.ChatFlow)")
    public void pointcut() {
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 输入参数
        Object[] args = joinPoint.getArgs();
        String session = (String) args[0]; // TODO
        final long sessionId;
        final String assistantType;
        if (!session.contains(":")) {
            assistantType = String.valueOf(LOCAL);
            sessionId = Long.parseLong(session);
        } else {
            String[] split = session.split(":");
            sessionId = Long.parseLong(split[0]);
            assistantType = split[1];
        }

        Map<?,?> map = (Map<?,?>) args[1];
        String message = (String) map.get("message");

        /*Long userId = UserContextUtil.getUserId();
        if(!userId.equals(sessionService.getById(sessionId).getUserId())){
            throw new BadRequestException("会话权限不足");
        }*/

        Message messageUser = Message.builder()
                .sessionId(sessionId)
                .senderType(SenderTypeEnum.USER)
                .messageType(MessageTypeEnum.TEXT)
                .created(LocalDateTime.now())
                .contents(message)
                .assistantType(AssistantTypeEnum.valueOf(assistantType))
                .build();
        sendMessage(messageUser);

        Object result = joinPoint.proceed();

        /*if (result instanceof ResponseResult<?> response) {
            log.info("ChatFlow 输出: code={}, msg={}, data={}",
                    response.getCode(), response.getMsg(), response.getData());

            Message messageAi = Message.builder()
                    .sessionId(sessionId)
                    .senderType(SenderTypeEnum.AI)
                    .messageType(MessageTypeEnum.TEXT)
                    .createTime(LocalDateTime.now())
                    .contents(response.getData().toString())
                    .assistantType(AssistantTypeEnum.valueOf(assistantType))
                    .build();
            sendMessage(messageAi);
        }*/

        if (!(result instanceof Flux<?> flux)) {
            throw new IllegalArgumentException("方法必须返回 Flux<String> 以支持流式输出");
        }

        StringBuffer buffer = new StringBuffer();
        return flux
                .timeout(Duration.ofSeconds(60))
                .doOnNext(buffer::append) // 非阻塞流
                .doOnComplete(() -> {
                    String finalAnswer = buffer.toString();
                    log.info("Chat 完成输出: {}", finalAnswer);
                    Message messageAi = Message.builder()
                            .sessionId(sessionId)
                            .senderType(SenderTypeEnum.AI)
                            .messageType(MessageTypeEnum.TEXT)
                            .created(LocalDateTime.now())
                            .contents(finalAnswer)
                            .assistantType(AssistantTypeEnum.valueOf(assistantType))
                            .build();
                    sendMessage(messageAi);
                })
                .doOnError(err -> log.error("Flux 流出错", err));
    }
}

