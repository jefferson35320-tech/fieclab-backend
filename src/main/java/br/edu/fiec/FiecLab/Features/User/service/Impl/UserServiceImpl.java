package br.edu.fiec.FiecLab.Features.User.service.Impl;

import br.edu.fiec.FiecLab.Features.User.Models.DTO.CreateUserRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.DTO.TokenRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.DTO.UserDTO;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.Role;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.User;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.UserStatus;
import br.edu.fiec.FiecLab.Features.User.repository.UserRepository;
import br.edu.fiec.FiecLab.Features.User.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @Override
    public void setToken(String email, TokenRequestDTO dto) {
        User user = getUserByEmail(email);
        user.setFcmToken(dto.token());
        userRepository.save(user);
    }

    @Override
    public void createUser(CreateUserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        User user = new User();
        user.setUuid(UUID.randomUUID().toString());
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setPosition(Role.CLIENTE);
        userRepository.save(user);
    }

    public void addUser(UserDTO dto) {
        User user = new User();
        user.setUuid(UUID.randomUUID().toString());
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setFcmToken(dto.getFcmToken());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(user);
    }


    /**
     * Cria usuários a partir de um CSV com cabeçalho. Colunas (por nome, em qualquer ordem):
     * name, email, password (obrigatórias), position (opcional: ADMIN, SUADMIN/SUPER_ADMIN ou CLIENTE;
     * padrão CLIENTE) e status (opcional: NOT_CONFIRMED ou CONFIRMED; padrão NOT_CONFIRMED).
     * Uma coluna "id" é ignorada, pois o id é gerado pelo banco.
     * O arquivo é validado por inteiro antes de salvar: se alguma linha tiver erro, nada é criado.
     */
    public List<User> importUsersFromCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo CSV enviado está vazio.");
        }

        List<User> usersToSave = new ArrayList<>();
        Set<String> emailsInFile = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("O CSV não tem cabeçalho.");
            }
            Map<String, Integer> columns = new HashMap<>();
            String[] header = headerLine.replace("﻿", "").split(",", -1);
            for (int i = 0; i < header.length; i++) {
                columns.put(header[i].trim().toLowerCase(), i);
            }
            for (String required : List.of("name", "email", "password")) {
                if (!columns.containsKey(required)) {
                    throw new IllegalArgumentException("Coluna obrigatória ausente no cabeçalho: " + required);
                }
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] data = line.split(",", -1);

                String name = csvValue(data, columns.get("name"));
                String email = csvValue(data, columns.get("email"));
                String password = csvValue(data, columns.get("password"));
                String position = csvValue(data, columns.get("position"));
                String status = csvValue(data, columns.get("status"));

                if (name == null || email == null || password == null) {
                    throw new IllegalArgumentException(
                            "Linha " + lineNumber + ": name, email e password são obrigatórios.");
                }
                email = email.toLowerCase();
                if (!emailsInFile.add(email)) {
                    throw new IllegalArgumentException(
                            "Linha " + lineNumber + ": e-mail repetido no arquivo: " + email);
                }
                if (userRepository.existsByEmail(email)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Linha " + lineNumber + ": e-mail já cadastrado: " + email);
                }

                User user = new User();
                user.setUuid(UUID.randomUUID().toString());
                user.setName(name);
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode(password));
                user.setPosition(parseRole(position, lineNumber));
                user.setStatus(parseStatus(status, lineNumber));
                usersToSave.add(user);
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao processar o arquivo CSV: " + e.getMessage(), e);
        }

        if (usersToSave.isEmpty()) {
            throw new IllegalArgumentException("O CSV não contém usuários.");
        }
        return userRepository.saveAll(usersToSave);
    }

    private static String csvValue(String[] data, Integer index) {
        if (index == null || index >= data.length) {
            return null;
        }
        String value = data[index].trim();
        return value.isEmpty() ? null : value;
    }

    private static UserStatus parseStatus(String value, int lineNumber) {
        if (value == null) {
            return UserStatus.NOT_CONFIRMED;
        }
        try {
            return UserStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Linha " + lineNumber + ": status inválido '" + value
                            + "' (use NOT_CONFIRMED ou CONFIRMED).");
        }
    }

    private static Role parseRole(String value, int lineNumber) {
        if (value == null) {
            return Role.CLIENTE;
        }
        String normalized = value.trim().toUpperCase().replaceFirst("^ROLE_", "");
        if (normalized.equals("SUPER_ADMIN")) {
            return Role.SUADMIN;
        }
        try {
            return Role.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Linha " + lineNumber + ": position inválida '" + value
                            + "' (use CLIENTE, ADMIN ou SUPER_ADMIN).");
        }
    }

    public User searchById(String id) {
        return userRepository.findById(id).orElseThrow();
    }

    public void deleteUserById(String id) {
        userRepository.deleteById(id);
    }

    public void UpdateUserById(String id, UserDTO dto) {
        User user = searchById(id);
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        userRepository.save(user);
    }

    public List<User> FindAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + username));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + email));
    }
}