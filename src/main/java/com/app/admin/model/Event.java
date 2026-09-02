package com.app.admin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "events", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class Event {
    @Id
    @SequenceGenerator(
            name = "events_id_generator",
            sequenceName = "events_id_seq",
            schema = "public",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "events_id_generator")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String title;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @Size(max = 255)
    @Column(length = 255)
    private String location;

    @Column(name = "is_online", nullable = false)
    private Boolean isOnline = false;

    @NotNull
    @Column(name = "start_date", nullable = false, columnDefinition = "timestamp without time zone")
    private LocalDateTime startDate;

    @Column(name = "end_date", columnDefinition = "timestamp without time zone")
    private LocalDateTime endDate;

    @Size(max = 255)
    @Column(length = 255)
    private String duration;

    @Size(max = 255)
    @Column(length = 255)
    private String price;

    @Size(max = 255)
    @Column(length = 255)
    private String level;

    @Size(max = 255)
    @Column(length = 255)
    private String source;

    @Size(max = 255)
    @Column(name = "source_url", length = 255)
    private String sourceUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "timestamp without time zone")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Size(max = 255)
    @Column(name = "register_url", length = 255)
    private String registerUrl;

    @Column(name = "event_type_id")
    private Long eventTypeId;

    @Size(max = 20)
    @Column(name = "delivery_mode", nullable = false, length = 20)
    private String deliveryMode = "IN_PERSON";

    @Column(name = "joining_url", columnDefinition = "text")
    private String joiningUrl;

    @Size(max = 50)
    @Column(name = "meeting_platform", length = 50)
    private String meetingPlatform;

    @Size(max = 150)
    @Column(name = "meeting_id", length = 150)
    private String meetingId;

    @Size(max = 100)
    @Column(name = "meeting_passcode", length = 100)
    private String meetingPasscode;

    @Size(max = 200)
    @Column(name = "venue_name", length = 200)
    private String venueName;

    @Size(max = 250)
    @Column(name = "address_line1", length = 250)
    private String addressLine1;

    @Size(max = 250)
    @Column(name = "address_line2", length = 250)
    private String addressLine2;

    @Size(max = 100)
    @Column(length = 100)
    private String city;

    @Size(max = 100)
    @Column(length = 100)
    private String state;

    @Size(max = 100)
    @Column(length = 100)
    private String country;

    @Size(max = 20)
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "map_url", columnDefinition = "text")
    private String mapUrl;

    @Size(max = 60)
    @Column(length = 60)
    private String timezone;

    @Size(max = 200)
    @Column(name = "organizer_name", length = 200)
    private String organizerName;

    @Column(name = "organizer_url", columnDefinition = "text")
    private String organizerUrl;

    @Size(max = 200)
    @Column(name = "contact_email", length = 200)
    private String contactEmail;

    @Column
    private Integer capacity;

    @Column(name = "created_by_user_id")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createdByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    @Column(name = "submitted_at", columnDefinition = "timestamp without time zone")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at", columnDefinition = "timestamp without time zone")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by_user_id")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long reviewedByUserId;

    @Column(name = "rejection_reason", columnDefinition = "text")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String reviewReason;

    @Column(name = "status_changed_at", nullable = false, columnDefinition = "timestamp without time zone")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime statusChangedAt;

    @Column(name = "published_at", columnDefinition = "timestamp without time zone")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime publishedAt;

    @Column(name = "cancelled_at", columnDefinition = "timestamp without time zone")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime cancelledAt;

    @Column(name = "registration_start_date", columnDefinition = "timestamp without time zone")
    private LocalDateTime registrationStartDate;

    @Column(name = "registration_end_date", columnDefinition = "timestamp without time zone")
    private LocalDateTime registrationEndDate;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "timestamp without time zone")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime updatedAt;
}
