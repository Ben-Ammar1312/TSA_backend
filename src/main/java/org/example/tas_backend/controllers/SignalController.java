package org.example.tas_backend.controllers;

import lombok.RequiredArgsConstructor;
import org.example.tas_backend.dtos.SignalMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class SignalController {

    private final SimpMessagingTemplate broker;

    @MessageMapping("/signal")
    public void relay(SignalMessage msg) {
        if (msg == null || msg.getTo() == null) return;
        broker.convertAndSend("/topic/signal/" + msg.getTo(), msg);
    }
}
