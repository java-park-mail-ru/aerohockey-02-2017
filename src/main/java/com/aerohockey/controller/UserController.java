package com.aerohockey.controller;

import com.aerohockey.model.UserProfile;
import com.aerohockey.responses.ErrorResponse;
import com.aerohockey.responses.LeaderboardResponse;
import com.aerohockey.responses.UserResponse;
import com.aerohockey.services.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;


import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Created by sergeybutorin on 20.02.17.
 */

@RestController
@CrossOrigin(origins = {"https://fastball.herokuapp.com", "http://localhost:3000", "http://127.0.0.1:3000"})
public class UserController {
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Autowired
    public UserController(AccountService accountService, PasswordEncoder passwordEncoder) {
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
    }

    @RequestMapping(path = "/api/signup", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity signup(@RequestBody UserProfile body, HttpSession httpSession) {
        final String login = body.getLogin();
        final String password = body.getPassword();
        final String email = body.getEmail();

        if (StringUtils.isEmpty(login)
                || StringUtils.isEmpty(password)
                || StringUtils.isEmpty(email)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ErrorResponse.WRONG_PARAMETERS));
        }

        if (httpSession.getAttribute("userId") != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ErrorResponse.SESSION_BUSY));
        }

        final UserProfile newUser = accountService.addUser(login, email, passwordEncoder.encode(password));
        if (newUser == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ErrorResponse.LOGIN_ALREADY_EXISTS));
        }
        httpSession.setAttribute("userId", newUser.getId());
        LOGGER.info("User {} registered", login);
        return ResponseEntity.ok(new UserResponse(newUser));
    }

    @RequestMapping(path = "/api/login", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity login(@RequestBody UserProfile body, HttpSession httpSession) {
        final String login = body.getLogin();
        final String password = body.getPassword();

        if (StringUtils.isEmpty(login)
                || StringUtils.isEmpty(password)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ErrorResponse.WRONG_PARAMETERS));
        }

        if (httpSession.getAttribute("userId") != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ErrorResponse.SESSION_BUSY));
        }

        final UserProfile user = accountService.getUserByLogin(login);

        if (user == null || !passwordEncoder.matches(password, user.getPassword()) || !user.getLogin().equals(login)) {
            LOGGER.info("User {} tried to login. Incorrect login/password", login);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ErrorResponse.INCORRECT_DATA));
        }

        httpSession.setAttribute("userId", user.getId());
        LOGGER.info("User {} logged in", login);
        return ResponseEntity.ok(new UserResponse(user));
    }

    @RequestMapping(path = "/api/user", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity getCurrentUser(HttpSession httpSession) {
        final UserProfile user = accountService.getUserById((Long) httpSession.getAttribute("userId"));
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ErrorResponse.NOT_AUTHORIZED));
        } else {
            return ResponseEntity.ok(new UserResponse(user));
        }
    }

    @RequestMapping(path = "/api/leaderboard", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity getLeadearboard(@RequestParam(name = "page", required = false, defaultValue = "1") int page) {
        final List<UserProfile> users = accountService.getLeaders(page);
        return ResponseEntity.ok(new LeaderboardResponse(users));
    }

    @RequestMapping(path = "/api/change-password", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity changePassword(@RequestBody UserProfile body, HttpSession httpSession) {
        final UserProfile user = accountService.getUserById((Long) httpSession.getAttribute("userId"));
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ErrorResponse.NOT_AUTHORIZED));
        }

        final String oldPassword = body.getOldPassword();
        final String newPassword = body.getPassword();

        if (StringUtils.isEmpty(oldPassword) || StringUtils.isEmpty(newPassword)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ErrorResponse.WRONG_PARAMETERS));
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Incorrect old password"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        accountService.changeData(user);
        LOGGER.info("Password for user {} successfully changed.", user.getLogin());
        return ResponseEntity.ok(new UserResponse(user));
    }

    @RequestMapping(path = "/api/change-user-data", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity changeUserData(@RequestBody UserProfile body, HttpSession httpSession) {
        final UserProfile user = accountService.getUserById((Long) httpSession.getAttribute("userId"));
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ErrorResponse.NOT_AUTHORIZED));
        } else {
            final String newEmail = body.getEmail();

            if (!StringUtils.isEmpty(newEmail)) {
                user.setEmail(newEmail);
            }

            accountService.changeData(user);
            LOGGER.info("User data for user {} successfully changed.", user.getLogin());
            return ResponseEntity.ok(new UserResponse(user));
        }
    }

    @RequestMapping(path = "/api/logout", method = RequestMethod.POST)
    public ResponseEntity logout(HttpSession httpSession) {
        if (httpSession.getAttribute("userId") != null) {
            LOGGER.info("User with id = {} logged out", httpSession.getAttribute("userId"));
            httpSession.invalidate();
            return ResponseEntity.ok("");
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ErrorResponse.NOT_AUTHORIZED));
    }
}
