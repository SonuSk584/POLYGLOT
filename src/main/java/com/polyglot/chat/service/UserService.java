package com.polyglot.chat.service;

import com.polyglot.chat.dto.UserDto;
import com.polyglot.chat.model.User;
import com.polyglot.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Add a new contact to the user's contact list
     */
    public User addContact(String userId, String contactUserId) {
        try {
            // Validate that both users exist
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("User not found: {}", userId);
                        return new RuntimeException("User not found");
                    });

            User contactUser = userRepository.findById(contactUserId)
                    .orElseThrow(() -> {
                        log.error("Contact user not found: {}", contactUserId);
                        return new RuntimeException("Contact user not found");
                    });

            // Prevent adding self as contact
            if (userId.equals(contactUserId)) {
                log.warn("User {} attempted to add themselves as contact", userId);
                throw new RuntimeException("Cannot add yourself as a contact");
            }

            // Check if contact already exists
            if (user.getContacts().contains(contactUserId)) {
                log.info("Contact {} already exists for user {}", contactUserId, userId);
                return user;
            }

            // Add the contact
            user.getContacts().add(contactUserId);

            // Save the updated user
            User savedUser = userRepository.save(user);
            log.info("Contact {} successfully added to user {}", contactUserId, userId);

            return savedUser;
        } catch (Exception e) {
            log.error("Error adding contact: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Remove a contact from the user's contact list
     */
    public User removeContact(String userId, String contactUserId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("User not found: {}", userId);
                        return new RuntimeException("User not found");
                    });

            if (user.getContacts().contains(contactUserId)) {
                user.getContacts().remove(contactUserId);
                User savedUser = userRepository.save(user);
                log.info("Contact {} removed from user {}", contactUserId, userId);
                return savedUser;
            } else {
                log.info("Contact {} not found for user {}", contactUserId, userId);
                return user;
            }
        } catch (Exception e) {
            log.error("Error removing contact: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get all contacts for a user with full user details
     */
    public List<UserDto> getContactsForUser(String userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("User not found: {}", userId);
                        return new RuntimeException("User not found");
                    });

            log.info("Fetching {} contacts for user {}", user.getContacts().size(), userId);

            return user.getContacts().stream()
                    .map(contactId -> {
                        try {
                            User contact = userRepository.findById(contactId).orElse(null);
                            return contact != null ? new UserDto(contact) : null;
                        } catch (Exception e) {
                            log.error("Error fetching contact {}: {}", contactId, e.getMessage());
                            return null;
                        }
                    })
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting contacts for user {}: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get only contact IDs for a user (lightweight query)
     */
    public List<String> getContactIdsForUser(String userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("User not found: {}", userId);
                        return new RuntimeException("User not found");
                    });

            return new ArrayList<>(user.getContacts());
        } catch (Exception e) {
            log.error("Error getting contact IDs for user {}: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Check if a user is in another user's contact list
     */
    public boolean isContact(String userId, String contactUserId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("User not found: {}", userId);
                        return new RuntimeException("User not found");
                    });

            return user.getContacts().contains(contactUserId);
        } catch (Exception e) {
            log.error("Error checking if {} is contact of {}: {}", contactUserId, userId, e.getMessage());
            return false;
        }
    }

    /**
     * Get user by ID
     */
    public User getUserById(String userId) {
        try {
            return userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("User not found: {}", userId);
                        return new RuntimeException("User not found");
                    });
        } catch (Exception e) {
            log.error("Error getting user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        try {
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        log.error("User not found with username: {}", username);
                        return new RuntimeException("User not found");
                    });
        } catch (Exception e) {
            log.error("Error getting user by username {}: {}", username, e.getMessage());
            throw e;
        }
    }
}