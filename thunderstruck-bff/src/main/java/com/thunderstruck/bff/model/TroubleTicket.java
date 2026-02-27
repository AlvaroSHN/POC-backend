package com.thunderstruck.bff.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("TROUBLE_TICKET")
public class TroubleTicket {
    @Id
    private Long id;
    private String externalId;
    private String description;
    private String severity;
    private String status;
    private LocalDateTime createdAt;
    private String relatedPartyId;
    private String ticketType;
}