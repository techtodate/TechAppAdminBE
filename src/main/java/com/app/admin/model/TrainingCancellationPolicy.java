package com.app.admin.model;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "training_cancellation_policies", schema = "public") @Getter @Setter @NoArgsConstructor
public class TrainingCancellationPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @JsonProperty(access = JsonProperty.Access.READ_ONLY) private Long id;
    @NotNull @Column(name = "training_offering_id", nullable = false) private Long trainingOfferingId;
    @NotNull @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "before_training_start_policy_id", nullable = false) private RefundPolicyRule beforeTrainingStartPolicy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "between_sessions_policy_id") private RefundPolicyRule betweenSessionsPolicy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "after_training_completion_policy_id") private RefundPolicyRule afterTrainingCompletionPolicy;
    @NotNull @Column(nullable = false) private Boolean active = true;
    @Column(name = "created_at", insertable = false, updatable = false) @JsonProperty(access = JsonProperty.Access.READ_ONLY) private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false) @JsonProperty(access = JsonProperty.Access.READ_ONLY) private LocalDateTime updatedAt;
}
