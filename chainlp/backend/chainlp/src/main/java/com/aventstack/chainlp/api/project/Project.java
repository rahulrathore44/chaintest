package com.aventstack.chainlp.api.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;

@Data
@ToString
@NoArgsConstructor
@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column
    private String name;

    @Column(name = "repository_name")
    private String repositoryName;

    @Column(name = "repository_url")
    private String repositoryUrl;

    @Column
    private String url;

    @PrePersist
    protected void onCreate() {
        createdAt = System.currentTimeMillis();
    }

    public Project(final String name) {
        this.name = name;
    }

}
