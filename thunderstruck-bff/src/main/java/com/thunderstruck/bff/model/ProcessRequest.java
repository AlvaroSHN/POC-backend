package com.thunderstruck.bff.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("PROCESS_REQUEST")
public class ProcessRequest {
    @Id
    private Long id;
    private String externalId;
    private String status;
    private String description;
    private LocalDateTime createdAt;

    // ADICIONE ESTES CAMPOS
    private String clientType;
    private String origin;
    
    // Envelope Pattern para Kafka
    @Data
    @Builder
    public static class KafkaEnvelope {
        private Metadata metadata;
        private Object data;
    }

    @Data
    @Builder
    public static class Metadata {
        private String eventId;
        private String eventType;
        private LocalDateTime timestamp;
        private String source;
    }
}
