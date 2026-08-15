package com.shiksha.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "teachers")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // user delete ya update hone par teacher ka account bhi sync rahe
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    @Column(length = 50)
    private String firstName;

    @Column(length = 50)
    private String lastName;

    @Column(length = 15)
    private String phone;

    @Column(length = 80)
    private String subject;

    private LocalDate joiningDate;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public String getFullName() {
        String f = firstName != null ? firstName : "";
        String l = lastName != null ? lastName : "";
        String full = (f + " " + l).trim();
        return full.isEmpty() ? (user != null ? user.getUsername() : "Teacher") : full;
    }
}
