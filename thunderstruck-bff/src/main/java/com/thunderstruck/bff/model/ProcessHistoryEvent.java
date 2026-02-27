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
@Table("PROCESS_HISTORY_EVENT")
public class ProcessHistoryEvent {

    @Id
    private Long id;
    private String externalId;
    private String stage;
    private String source;
    private String details;
    private String previousStatus;
    private String currentStatus;
    private LocalDateTime createdAt;
}
