package com.nearshare.api.repository;

import com.nearshare.api.model.Message;
import com.nearshare.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findBySenderOrReceiver(User sender, User receiver);
    @Query("select m from Message m where (m.sender = :a and m.receiver = :b) or (m.sender = :b and m.receiver = :a) order by m.timestamp asc")
    List<Message> conversation(@Param("a") User a, @Param("b") User b);
    List<Message> findByReceiverOrderByTimestampDesc(User receiver);
    List<Message> findBySenderOrderByTimestampDesc(User sender);
}