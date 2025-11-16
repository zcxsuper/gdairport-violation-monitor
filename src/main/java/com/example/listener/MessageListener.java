package com.example.listener;

import com.example.config.RabbitConfig;
import com.example.domain.entity.Message;
import com.example.service.MessageService;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = RabbitConfig.MESSAGE_QUEUE)
public class MessageListener {

    @Resource
    private MessageService messageService;

    @RabbitHandler
    public void handleMessage(Message message) {
        messageService.save(message);
    }
}
