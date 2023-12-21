package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.configurations.JwtUtil;
import com.project.examportalbackend.exception.exceptions.ResourceNotFoundException;
import com.project.examportalbackend.models.*;
import com.project.examportalbackend.models.dto.request.LoginOtpRequestDto;
import com.project.examportalbackend.models.dto.request.UserRequestDto;
import com.project.examportalbackend.repository.RoleRepository;
import com.project.examportalbackend.repository.SubjectRepository;
import com.project.examportalbackend.repository.UserRepository;
import com.project.examportalbackend.services.AuthService;
import com.project.examportalbackend.utils.constants.Roles;
import lombok.extern.java.Log;
import net.bytebuddy.utility.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.mail.IllegalWriteException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.file.AccessDeniedException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.springframework.data.util.Pair.of;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public User registerUserService(UserRequestDto userRequestDto) throws MessagingException, UnsupportedEncodingException {

        User temp = userRepository.findByEmail(userRequestDto.getEmail());
        if (temp != null) {
            throw new IllegalArgumentException("User with email: " + userRequestDto.getEmail() + " already exists;");
        }

        Role role = getRole(userRequestDto.getRole());
        userRequestDto.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));

        User user = new User(
                userRequestDto.getFirstName(),
                userRequestDto.getLastName(),
                userRequestDto.getEmail(),
                userRequestDto.getPassword(),
                userRequestDto.getPhoneNumber(),
                role);

//        Pair<String, Date> otpPair = generateOneTimePassword();
//        user.setOneTimePassword(passwordEncoder.encode(otpPair.getFirst()));
//        user.setOtpGeneratedTime(otpPair.getSecond());
//        user.setResetPassword(1);

        User savedUser = userRepository.save(user);
        //sendOTPEmail(user, otpPair.getFirst());
        return savedUser;
    }

    @Override
    public User updateUser(UserRequestDto userRequestDto) {

        if (userRequestDto.getUserId() == -1) {
            throw new IllegalArgumentException("You must pass the id of the user you wish to update");
        }
        getUser(userRequestDto.getUserId());
        Role role = getRole(userRequestDto.getRole());

        userRequestDto.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
        return userRepository.save(new User(
                userRequestDto.getUserId(),
                userRequestDto.getFirstName(),
                userRequestDto.getLastName(),
                userRequestDto.getEmail(),
                userRequestDto.getPassword(),
                userRequestDto.getPhoneNumber(),
                role));
    }

    @Override
    public void deleteUser(long userId) {
        User user = getUser(userId);
        if (Objects.equals(user.getRole().getRoleName(), Roles.PROFESSOR.toString())) {
            Subject subject = subjectRepository.findByProfessorUserId(user.getUserId());
            if (subject != null) {
                throw new IllegalArgumentException("Can't delete professor "
                        + user.getFullName()
                        + " because he is assigned to a subject");
            }
        }
        if (Objects.equals(user.getRole().getRoleName(), Roles.ADMIN.toString())) {
            throw new IllegalArgumentException("Can't delete the admin");
        }
        userRepository.delete(user);
    }

    public Role getRole(String roleName) {
        Optional<Role> role = roleRepository.findById(roleName);
        if (role.isEmpty()) {
            throw new ResourceNotFoundException("Role " + roleName + "doesn't exist");
        }
        return role.get();
    }

    public LoginResponse loginUserService(LoginRequest loginRequest) {
        authenticate(loginRequest.getEmail(), loginRequest.getPassword());
        UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(loginRequest.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        return new LoginResponse(userRepository.findByEmail(loginRequest.getEmail()), token);
    }

    public void resetPassword(long userId, String password) {
        User user = getUser(userId);
        user.setPassword(password);
        user.setResetPassword(0);
        userRepository.save(user);
        clearOTP(userId);
    }

//    public void requestPasswordChange()

    public LoginResponse loginOtpUserService(LoginOtpRequestDto loginOtpRequestDto) {
        authenticate(loginOtpRequestDto.getEmail(), loginOtpRequestDto.getOtp());
        UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(loginOtpRequestDto.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        User user = userRepository.findByEmail(loginOtpRequestDto.getEmail());
        return new LoginResponse(user, token);
    }

    public Role getUserRoleByUserId(long userId) throws ResourceNotFoundException {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            return user.get().getRole();
        } else {
            throw new ResourceNotFoundException("User with id " + userId + " doesn't exist");
        }
    }

    @Override
    public User getUser(long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User with id:" + userId + "doesn't exist");
        }
        return user.get();
    }

    @Override
    public User getUserFromToken() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public void verifyUserRole(long userId, String roleName) throws AccessDeniedException {
        if (!getUserRoleByUserId(userId).getRoleName().equals(roleName)) {
            throw new AccessDeniedException("User must be a " + roleName);
        }
    }

    private void authenticate(String username, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException e) {
            throw new DisabledException("User " + username + " is disabled", e);
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Incorrect email or password", e);
        }
    }

    @Override
    public Pair<String, Date> generateOneTimePassword() {
        String otp = RandomString.make(8);
        return Pair.of(otp, new Date());
    }

    @Override
    public void setNewOneTimePassword(long userId) throws MessagingException, UnsupportedEncodingException {
        User user = getUser(userId);
        if (user.getRole().getRoleName().equals(Roles.ADMIN.toString())) {
            throw new IllegalArgumentException("Can't generate otp for admin");
        }
        Pair<String, Date> otpPair = generateOneTimePassword();
        user.setOneTimePassword(passwordEncoder.encode(otpPair.getFirst()));
        user.setOtpGeneratedTime(otpPair.getSecond());
        user.setResetPassword(1);
        userRepository.save(user);
        sendOTPEmail(user, user.getOneTimePassword());
    }

    @Override
    public void sendOTPEmail(User user, String otp) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom("tsexampotal@gmail.com", "Exam Portal");
        helper.setTo(user.getEmail());

        String subject = "Here's your One Time Password (OTP) - Expires in 60 minutes!";

        String content = "<p>Hello " + user.getFullName() + "</p>"
                + "<p>For security reason, you're required to use the following "
                + "One Time Password to login:</p>"
                + "<p><b>" + otp + "</b></p>"
                + "<br>"
                + "<p>Note: this OTP is set to expire in 60 minutes.</p>";

        helper.setSubject(subject);

        helper.setText(content, true);

        mailSender.send(message);
    }

    @Override
    public void clearOTP(long userId) {
        User user = getUser(userId);
        user.setOneTimePassword(null);
        user.setOtpGeneratedTime(null);
        userRepository.save(user);
    }
}