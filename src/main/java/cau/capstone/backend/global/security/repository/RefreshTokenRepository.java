package cau.capstone.backend.global.security.repository;

import cau.capstone.backend.global.security.Entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface  RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByKey(String key);

//    Optional<RefreshToken> findByUserId(Long userId);
}