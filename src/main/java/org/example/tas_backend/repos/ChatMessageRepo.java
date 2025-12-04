package org.example.tas_backend.repos;

import org.example.tas_backend.entities.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepo extends JpaRepository<ChatMessageEntity, String> {

    @Query("""
        select m from ChatMessageEntity m
        where (m.sender = :user and m.recipient = :other) or (m.sender = :other and m.recipient = :user)
        order by m.createdAt desc
    """)
    List<ChatMessageEntity> findThread(@Param("user") String user, @Param("other") String other);

    @Query("""
        select m from ChatMessageEntity m
        where m.sender = :user or m.recipient = :user
        order by m.createdAt desc
    """)
    List<ChatMessageEntity> findRecentForUser(@Param("user") String user);
}
