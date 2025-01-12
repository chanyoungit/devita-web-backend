package com.devita.domain.category.domain;

import com.devita.common.entity.BaseEntity;
import com.devita.domain.todo.domain.Todo;
import com.devita.domain.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@ToString
public class Category extends BaseEntity {
    @Id
    @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String name;

    private String color;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Todo> todoEntities = new ArrayList<>();

    @Builder
    public Category(User user, String name, String color) {
        this.user = user;
        this.name = name;
        this.color = color;
    }

    public void setNameAndColor(String name, String color){
        this.name = name;
        this.color = color;
    }

}