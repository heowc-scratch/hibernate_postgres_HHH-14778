package com.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Modifying
    @Query(value = "UPDATE message SET count = :count WHERE id = :id", nativeQuery = true)
    void update(@Param("count") Long count, @Param("id") Long id);
}
