package com.kalana.kanbanBoard.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_item_id", nullable = false)
    private WorkItem workItem;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "public_id", nullable = false)
    private String publicId;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "original_name", length = 200)
    private String originalName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
