package com.vicinity24.api.core.repository;

import com.vicinity24.api.core.model.Message;
import com.vicinity24.api.core.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    @Query("select m from Message m where m.sender = :sender or m.receiver = :receiver")
    List<Message> findBySenderOrReceiver(@Param("sender") User sender, @Param("receiver") User receiver);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("select m from Message m where (m.sender = :user and m.deletedBySender = false) or (m.receiver = :user and m.deletedByReceiver = false)")
    List<Message> findBySenderOrReceiverAndNotDeleted(@Param("user") User user);

    @Query("select m from Message m join fetch m.sender join fetch m.receiver where ((m.sender = :a and m.receiver = :b) or (m.sender = :b and m.receiver = :a)) and ((m.sender = :a and m.deletedBySender = false) or (m.receiver = :a and m.deletedByReceiver = false)) order by m.timestamp asc")
    List<Message> conversationForUser(@Param("a") User a, @Param("b") User b);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("select m from Message m where m.receiver = :receiver and m.deletedByReceiver = false order by m.timestamp desc")
    List<Message> findInboxForUser(@Param("receiver") User receiver);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("select m from Message m where m.sender = :sender and m.deletedBySender = false order by m.timestamp desc")
    List<Message> findOutboxForUser(@Param("sender") User sender);
}
