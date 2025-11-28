package org.example.tas_backend.dtos;

import lombok.Data;

@Data
public class SignalMessage {
    private String type;      // offer | answer | ice
    private String from;
    private String to;
    private String sdp;
    private String candidate;
}
