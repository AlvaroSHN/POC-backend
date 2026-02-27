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
@Table("CUSTOMER_INTERACTION")
public class CustomerInteraction {

    @Id
    private Long id;
    private String interactionId;
    private String externalId;
    private LocalDateTime creationDate;
    private String channel;
    private String reason;
    private String customerId;
}
