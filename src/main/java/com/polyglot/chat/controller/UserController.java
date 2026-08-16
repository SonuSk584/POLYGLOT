package com.polyglot.chat.controller;

import com.polyglot.chat.dto.UserDto;
import com.polyglot.chat.model.User;
import com.polyglot.chat.security.CurrentUser;
import com.polyglot.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Add a contact to the current user's contact list
     * @param request - should contain contactUserId
     * @param currentUser - authenticated user
     * @return updated user with new contact added
     */
    @PostMapping("/add-contact")
    public ResponseEntity<UserDto> addContact(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CurrentUser currentUser) {

        String contactUserId = request.get("contactUserId");

        if (contactUserId == null || contactUserId.isEmpty()) {
            log.warn("Contact user ID is null or empty");
            return ResponseEntity.badRequest().build();
        }

        log.info("User {} adding contact {}", currentUser.getId(), contactUserId);

        User updatedUser = userService.addContact(currentUser.getId(), contactUserId);

        if (updatedUser != null) {
            log.info("Contact {} successfully added to user {}", contactUserId, currentUser.getId());
            return ResponseEntity.ok(new UserDto(updatedUser));
        }

        log.error("Failed to add contact {} for user {}", contactUserId, currentUser.getId());
        return ResponseEntity.badRequest().build();
    }

    /**
     * Remove a contact from the current user's contact list
     * @param request - should contain contactUserId
     * @param currentUser - authenticated user
     * @return updated user with contact removed
     */
    @PostMapping("/remove-contact")
    public ResponseEntity<UserDto> removeContact(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CurrentUser currentUser) {

        String contactUserId = request.get("contactUserId");

        if (contactUserId == null || contactUserId.isEmpty()) {
            log.warn("Contact user ID is null or empty");
            return ResponseEntity.badRequest().build();
        }

        log.info("User {} removing contact {}", currentUser.getId(), contactUserId);

        User updatedUser = userService.removeContact(currentUser.getId(), contactUserId);

        if (updatedUser != null) {
            log.info("Contact {} successfully removed from user {}", contactUserId, currentUser.getId());
            return ResponseEntity.ok(new UserDto(updatedUser));
        }

        log.error("Failed to remove contact {} for user {}", contactUserId, currentUser.getId());
        return ResponseEntity.badRequest().build();
    }

    /**
     * Get all contacts for the current user
     * @param currentUser - authenticated user
     * @return list of user's contacts
     */
    @GetMapping("/contacts")
    public ResponseEntity<List<UserDto>> getContacts(
            @AuthenticationPrincipal CurrentUser currentUser) {

        log.info("Getting contacts for user {}", currentUser.getId());

        List<UserDto> contacts = userService.getContactsForUser(currentUser.getId());
        log.info("Retrieved {} contacts for user {}", contacts.size(), currentUser.getId());

        return ResponseEntity.ok(contacts);
    }

    /**
     * Check if a specific user is in the current user's contact list
     * @param contactUserId - ID of user to check
     * @param currentUser - authenticated user
     * @return true if user is a contact, false otherwise
     */
    @GetMapping("/is-contact/{contactUserId}")
    public ResponseEntity<Map<String, Boolean>> isContact(
            @PathVariable String contactUserId,
            @AuthenticationPrincipal CurrentUser currentUser) {

        log.info("Checking if {} is a contact of user {}", contactUserId, currentUser.getId());

        boolean isContact = userService.isContact(currentUser.getId(), contactUserId);

        return ResponseEntity.ok(Map.of("isContact", isContact));
    }
}