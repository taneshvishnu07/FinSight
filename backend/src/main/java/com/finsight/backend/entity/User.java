package com.finsight.backend.entity;

import java.util.ArrayList;
import java.util.List;
import com.finsight.backend.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email")
        }
)
/**
 * Application user, stored in the `users` table (email must be unique). The password
 * column holds a BCrypt hash, never plain text.
 *
 * Owns one FinancialProfile and many Transactions; deleting a user cascades to both.
 */
public class User extends BaseEntity {

    @NotBlank
    @Column(nullable = false, length = 100)
    private String fullName;

    @Email
    @NotBlank
    @Column(nullable = false, length = 150)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    // Defaults: every new user gets role USER and is enabled.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private Boolean enabled = true;

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private FinancialProfile financialProfile;

    @OneToMany(
        mappedBy = "user",
        cascade = CascadeType.ALL,
        orphanRemoval = true
   )
    private List<Transaction> transactions = new ArrayList<>();

}