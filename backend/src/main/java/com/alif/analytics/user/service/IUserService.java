package com.alif.analytics.user.service;

import com.alif.analytics.dto.UserDto;

import java.util.Optional;

public interface IUserService {

    /**
     * Search for a user by email address
     * @param email the email address to search for
     * @return Optional containing UserDto if found
     */
    Optional<UserDto> searchUserByEmail(String email);

    /**
     * Elevate a user's role to ROLE_EMPLOYER
     * @param userId the ID of the user to elevate
     * @return updated UserDto
     */
    UserDto elevateToEmployer(Long userId);

}
