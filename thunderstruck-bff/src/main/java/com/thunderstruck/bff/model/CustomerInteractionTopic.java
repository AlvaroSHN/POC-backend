package com.thunderstruck.bff.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("CUSTOMER_INTERACTION_TOPIC")
public class CustomerInteractionTopic {

    @Id
    private Long id;
    private String topicId;
    private String interactionId;
    private String topicType;
    private String description;
    private String status;
    private LocalDateTime createdAt;
}
