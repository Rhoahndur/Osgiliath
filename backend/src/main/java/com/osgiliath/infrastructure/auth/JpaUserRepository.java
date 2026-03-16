package com.osgiliath.infrastructure.auth;

import com.osgiliath.domain.auth.User;
import com.osgiliath.domain.auth.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUserRepository extends JpaRepository<User, UUID>, UserRepository {

    @Override
    Optional<User> findByUsername(String username);

    @Override
    Optional<User> findById(UUID id);

    @Override
    boolean existsByUsername(String username);

    @Override
    boolean existsByEmail(String email);
}
