package com.devita.domain.todo.repository;

import com.devita.domain.todo.domain.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT t FROM Todo t WHERE t.user.id = :userId AND t.category.name = :categoryName AND t.date = :currentDate")
    Todo findTodosByUserIdAndCategoryNameAndDate(Long userId, String categoryName, LocalDate currentDate);

}