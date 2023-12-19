package com.project.examportalbackend.controllers;

import com.project.examportalbackend.models.Subject;
import com.project.examportalbackend.models.User;
import com.project.examportalbackend.repository.UserRepository;
import com.project.examportalbackend.services.AuthService;
import com.project.examportalbackend.services.SubjectService;
import com.project.examportalbackend.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/subject")
public class SubjectController {

    @Autowired
    private SubjectService subjectService;
    private final UserRepository userRepository;
    @Autowired
    public SubjectController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;


//    @PostMapping("/")
//    public ResponseEntity<?> addSubject(@RequestBody Subject subject) {
//        return ResponseEntity.ok(subjectService.addSubject(subject));
//    }

    @PreAuthorize("hasAuthority('STUDENT')")
    @GetMapping("/student")
    public ResponseEntity<List<Subject>> getSubjects() throws AccessDeniedException {
        User student = authService.getUserFromToken();
        return ResponseEntity.ok(subjectService.getSubjectsByStudentId(student.getUserId()));
    }

//    @GetMapping("/subjects")
//    public ResponseEntity<List<Subject>> getAllSubjects(){
//        return ResponseEntity.ok(subjectService.getSubjects());
//    }
//
//    @GetMapping("/{subjectId}")
//    public ResponseEntity<?> getSubject(@PathVariable Long subjectId) {
//        return ResponseEntity.ok(subjectService.getSubject(subjectId));
//    }
//
//    @PutMapping("/{subjectId}")
//    public ResponseEntity<?> updateSubject(@PathVariable Long subjectId, @RequestBody Subject subject) {
//        if (subjectService.getSubject(subjectId) != null) {
//            return ResponseEntity.ok(subjectService.updateSubject(subject));
//        }
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Subject with id : " + String.valueOf(subjectId) + ", doesn't exists");
//    }
//
//    @DeleteMapping("/{subjectId}")
//    public ResponseEntity<?> deleteSubject(@PathVariable Long subjectId) {
//        subjectService.deleteSubject(subjectId);
//        return ResponseEntity.ok(true);
//    }

//    @GetMapping("/users")
//    public List<User> getAllUsers() {
//        return userRepository.findAll();
//    }
//
//
//
////
//
//    @GetMapping("/professor/{subjectId}")
//    public ResponseEntity<?> getProfesor(@PathVariable Long subjectId) {
//        return ResponseEntity.ok(userService.getProfessor(subjectId));
//    }
//    @GetMapping("/profesors")
//    public ResponseEntity<?> getAllProfesors(){
//        return ResponseEntity.ok(userService.getAllProfesors());
//    }
//
//    @GetMapping("/students")
//    public ResponseEntity<?> getAllStudents(){
//        return ResponseEntity.ok(userService.getAllStudents());
//    }

    @PreAuthorize("hasAuthority('PROFESSOR')")
    @GetMapping("/professor")
    public ResponseEntity<Subject> getProfessorSubject() throws AccessDeniedException {
        User professor = authService.getUserFromToken();
        return ResponseEntity.ok(subjectService.getSubjectFromProfessor(professor.getUserId()));
    }



}