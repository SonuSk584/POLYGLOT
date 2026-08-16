package com.polyglot.chat.repository;

import com.polyglot.chat.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByMobileNumber(String mobileNumber);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByAuthDetails_ProviderAndAuthDetails_ProviderId(User.AuthProvider provider, String providerId);
    
    boolean existsByUsername(String username);
    
    boolean existsByMobileNumber(String mobileNumber);
}