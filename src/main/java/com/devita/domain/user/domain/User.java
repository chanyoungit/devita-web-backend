package com.devita.domain.user.domain;

import com.devita.common.entity.BaseEntity;
import com.devita.domain.category.domain.Category;
import com.devita.domain.post.domain.Post;
import com.devita.domain.todo.domain.Todo;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    @Setter
    private Long id;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuthProvider provider;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Todo> todoEntities;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Category> categories;

    // 내가 작성한 게시물 목록을 확인하기 위해 작성
    @OneToMany(mappedBy = "writer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts;

    @ElementCollection
    @CollectionTable(
            name = "user_preferred_categories",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "category_name")
    private List<PreferredCategory> preferredCategories = new ArrayList<>();

    private String profileImage;

    @Builder
    public User(String email, String nickname, AuthProvider provider, String profileImage) {
        this.email = email;
        this.nickname = nickname;
        this.provider = provider;
        this.profileImage = profileImage;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePreferredCategories(List<PreferredCategory> categories) {
        this.preferredCategories.clear();
        this.preferredCategories.addAll(categories);
    }
}