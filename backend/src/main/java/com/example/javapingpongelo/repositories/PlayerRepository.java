package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<Player, UUID> {
    Player findByEmail(String email);
    
    Player findByUsername(String username);
    
    @Query("SELECT p.username FROM Player p")
    List<String> findAllUsernames();
}
