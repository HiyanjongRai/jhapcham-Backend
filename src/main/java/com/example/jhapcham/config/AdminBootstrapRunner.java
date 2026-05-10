package com.example.jhapcham.config;

import com.example.jhapcham.delivery.Courier;
import com.example.jhapcham.delivery.CourierRepository;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.Status;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private static final String ADMIN_EMAIL = "raihenjong332@gmail.com";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD_HASH = "$2a$10$5ygO30qHaWpcDBLlHbS0tOYJ4psUl3Hg6Z2WX.vjYUMcHNouITMkW";
    private static final String ADMIN_CONTACT = "9765006485";
    private static final String ADMIN_FULL_NAME = "Main Admin";
    private static final String BOOTSTRAP_COURIER_EMAIL = "abc@gmail.com";
    private static final String BOOTSTRAP_COURIER_PASSWORD_HASH = "$2a$10$5ygO30qHaWpcDBLlHbS0tOYJ4psUl3Hg6Z2WX.vjYUMcHNouITMkW";
    private static final String BOOTSTRAP_COURIER_NAME = "Default Courier";
    private static final String BOOTSTRAP_COURIER_PHONE = "9800000000";
    private static final String BOOTSTRAP_COURIER_DISTRICT = "Kathmandu";
    private static final String BOOTSTRAP_COURIER_VEHICLE = "Bike";

    private final UserRepository userRepository;
    private final CourierRepository courierRepository;

    @Override
    public void run(String... args) {
        bootstrapAdmin();
        bootstrapCourier();
    }

    private void bootstrapAdmin() {
        User adminByEmail = userRepository.findByEmail(ADMIN_EMAIL).orElse(null);
        User adminByUsername = userRepository.findByUsername(ADMIN_USERNAME).orElse(null);
        User admin = adminByEmail != null ? adminByEmail : adminByUsername;

        if (admin != null) {
            boolean updated = false;

            if (adminByUsername == null && !ADMIN_USERNAME.equals(admin.getUsername())) {
                admin.setUsername(ADMIN_USERNAME);
                updated = true;
            }
            if (adminByEmail == null && !ADMIN_EMAIL.equals(admin.getEmail())) {
                admin.setEmail(ADMIN_EMAIL);
                updated = true;
            }
            if (!ADMIN_FULL_NAME.equals(admin.getFullName())) {
                admin.setFullName(ADMIN_FULL_NAME);
                updated = true;
            }
            if (!ADMIN_CONTACT.equals(admin.getContactNumber())) {
                admin.setContactNumber(ADMIN_CONTACT);
                updated = true;
            }
            if (admin.getRole() != Role.ADMIN) {
                admin.setRole(Role.ADMIN);
                updated = true;
            }
            if (admin.getStatus() != Status.ACTIVE) {
                admin.setStatus(Status.ACTIVE);
                updated = true;
            }

            String storedPassword = admin.getPassword();
            if (!ADMIN_PASSWORD_HASH.equals(storedPassword)) {
                admin.setPassword(ADMIN_PASSWORD_HASH);
                updated = true;
            }

            if (updated) {
                userRepository.save(admin);
                logger.info("Bootstrap admin account repaired for email {}", ADMIN_EMAIL);
            } else {
                logger.info("Bootstrap admin already exists for email {}", ADMIN_EMAIL);
            }

            if (adminByEmail != null && adminByUsername != null && !adminByEmail.getId().equals(adminByUsername.getId())) {
                logger.warn(
                        "Admin bootstrap detected separate records for email {} and username {}. " +
                                "The account was repaired without merging records. Clean up duplicate users manually.",
                        ADMIN_EMAIL,
                        ADMIN_USERNAME);
            }
            return;
        }

        if (userRepository.existsByUsername(ADMIN_USERNAME)) {
            logger.warn("Bootstrap admin was not created because username {} is already in use", ADMIN_USERNAME);
            return;
        }

        User adminToCreate = User.builder()
                .username(ADMIN_USERNAME)
                .fullName(ADMIN_FULL_NAME)
                .email(ADMIN_EMAIL)
                .contactNumber(ADMIN_CONTACT)
                .password(ADMIN_PASSWORD_HASH)
                .role(Role.ADMIN)
                .status(Status.ACTIVE)
                .build();

        userRepository.save(adminToCreate);
        logger.info("Bootstrap admin created for email {}", ADMIN_EMAIL);
    }

    private void bootstrapCourier() {
        Courier courier = courierRepository.findByEmailIgnoreCase(BOOTSTRAP_COURIER_EMAIL).orElse(null);

        if (courier != null) {
            boolean updated = false;

            if (!BOOTSTRAP_COURIER_NAME.equals(courier.getFullName())) {
                courier.setFullName(BOOTSTRAP_COURIER_NAME);
                updated = true;
            }
            if (!BOOTSTRAP_COURIER_PHONE.equals(courier.getPhoneNumber())) {
                courier.setPhoneNumber(BOOTSTRAP_COURIER_PHONE);
                updated = true;
            }
            if (!BOOTSTRAP_COURIER_DISTRICT.equals(courier.getCurrentDistrict())) {
                courier.setCurrentDistrict(BOOTSTRAP_COURIER_DISTRICT);
                updated = true;
            }
            if (!BOOTSTRAP_COURIER_VEHICLE.equals(courier.getVehicleType())) {
                courier.setVehicleType(BOOTSTRAP_COURIER_VEHICLE);
                updated = true;
            }
            if (!courier.isActive()) {
                courier.setActive(true);
                updated = true;
            }
            if (!BOOTSTRAP_COURIER_PASSWORD_HASH.equals(courier.getPasswordHash())) {
                courier.setPasswordHash(BOOTSTRAP_COURIER_PASSWORD_HASH);
                updated = true;
            }

            if (updated) {
                courierRepository.save(courier);
                logger.info("Bootstrap courier account repaired for email {}", BOOTSTRAP_COURIER_EMAIL);
            } else {
                logger.info("Bootstrap courier already exists for email {}", BOOTSTRAP_COURIER_EMAIL);
            }
            return;
        }

        Courier courierToCreate = Courier.builder()
                .fullName(BOOTSTRAP_COURIER_NAME)
                .email(BOOTSTRAP_COURIER_EMAIL)
                .phoneNumber(BOOTSTRAP_COURIER_PHONE)
                .passwordHash(BOOTSTRAP_COURIER_PASSWORD_HASH)
                .active(true)
                .currentDistrict(BOOTSTRAP_COURIER_DISTRICT)
                .vehicleType(BOOTSTRAP_COURIER_VEHICLE)
                .build();

        courierRepository.save(courierToCreate);
        logger.info("Bootstrap courier created for email {}", BOOTSTRAP_COURIER_EMAIL);
    }
}
