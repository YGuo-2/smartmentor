package com.tricia.smartmentor.service;

import com.tricia.smartmentor.common.BusinessException;
import com.tricia.smartmentor.config.JwtUtil;
import com.tricia.smartmentor.config.UserPrincipal;
import com.tricia.smartmentor.dto.*;
import com.tricia.smartmentor.entity.Student;
import com.tricia.smartmentor.entity.StudentProfile;
import com.tricia.smartmentor.repository.StudentProfileRepository;
import com.tricia.smartmentor.repository.StudentRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MailService mailService;

    public AuthService(StudentRepository studentRepository,
                       StudentProfileRepository studentProfileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       MailService mailService) {
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.mailService = mailService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 验证邮箱验证码
        if (!mailService.verifyCaptcha(request.getEmail(), request.getCode())) {
            throw new BusinessException(400, "验证码错误或已过期");
        }

        String role = normalizeRole(request.getRole());
        if ("student".equals(role)) {
            return registerStudent(request);
        } else {
            throw new BusinessException(400, "仅支持学生账号");
        }
    }

    private AuthResponse registerStudent(RegisterRequest request) {
        if (studentRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(409, "用户名已存在");
        }

        Student student = new Student();
        student.setUsername(request.getUsername());
        student.setPassword(passwordEncoder.encode(request.getPassword()));
        student.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        student.setGrade(request.getGrade());
        student.setSchool(request.getSchool());
        student.setEmail(request.getEmail());
        student = studentRepository.save(student);

        StudentProfile profile = new StudentProfile();
        profile.setStudentId(student.getId());
        studentProfileRepository.save(profile);

        String token = jwtUtil.generateToken(student.getId(), student.getUsername(), "student");

        return AuthResponse.builder()
                .token(token)
                .userId(student.getId())
                .username(student.getUsername())
                .nickname(student.getNickname())
                .email(student.getEmail())
                .role("student")
                .grade(student.getGrade())
                .school(student.getSchool())
                .avatarUrl(student.getAvatarUrl())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String role = normalizeRole(request.getRole());
        if ("student".equals(role)) {
            Student student = studentRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));
            if (!passwordEncoder.matches(request.getPassword(), student.getPassword())) {
                throw new BusinessException(401, "用户名或密码错误");
            }
            String token = jwtUtil.generateToken(student.getId(), student.getUsername(), "student");
            return AuthResponse.builder()
                    .token(token)
                    .userId(student.getId())
                    .username(student.getUsername())
                    .nickname(student.getNickname())
                    .email(student.getEmail())
                    .role("student")
                    .grade(student.getGrade())
                    .school(student.getSchool())
                    .avatarUrl(student.getAvatarUrl())
                    .build();
        } else {
            throw new BusinessException(400, "仅支持学生账号");
        }
    }

    public CurrentUserResponse getCurrentUser(UserPrincipal principal) {
        String role = principal.getRole();
        if ("student".equals(role)) {
            Student student = studentRepository.findById(principal.getUserId())
                    .orElseThrow(() -> new BusinessException(404, "用户不存在"));
            StudentProfile profile = studentProfileRepository.findByStudentId(student.getId())
                    .orElse(null);

            CurrentUserResponse.CurrentUserResponseBuilder builder = CurrentUserResponse.builder()
                    .userId(student.getId())
                    .username(student.getUsername())
                    .nickname(student.getNickname())
                    .email(student.getEmail())
                    .role("student")
                    .grade(student.getGrade())
                    .school(student.getSchool())
                    .avatarUrl(student.getAvatarUrl())
                    .createdAt(student.getCreatedAt());

            if (profile != null) {
                builder.profile(CurrentUserResponse.ProfileSummary.builder()
                        .overallMastery(profile.getOverallMastery())
                        .level(profile.getLevel())
                        .experiencePoints(profile.getExperiencePoints())
                        .streakDays(profile.getStreakDays())
                        .totalStudyHours(profile.getTotalStudyHours())
                        .lastDiagnosticAt(profile.getLastDiagnosticAt())
                        .majorDirection(profile.getMajorDirection())
                        .educationLevel(profile.getEducationLevel())
                        .currentCourse(profile.getCurrentCourse())
                        .learningGoal(profile.getLearningGoal())
                        .foundationLevel(profile.getFoundationLevel())
                        .resourcePreference(profile.getResourcePreference())
                        .academicInterest(profile.getAcademicInterest())
                        .build());
            }
            return builder.build();
        } else {
            throw new BusinessException(400, "仅支持学生账号");
        }
    }

    private String normalizeRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return "student";
        }
        return role.trim().toLowerCase();
    }
}
