package br.edu.fiec.FiecLab.Features.User.controller;

import br.edu.fiec.FiecLab.Features.User.Models.DTO.CreateUserRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.DTO.TokenRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.DTO.UserDTO;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.User;
import br.edu.fiec.FiecLab.Features.User.service.Backup;
import br.edu.fiec.FiecLab.Features.User.service.Impl.UserServiceImpl;
import br.edu.fiec.FiecLab.Features.User.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
@RestController
@RequestMapping(value = "/users")
@AllArgsConstructor
public class UserController {

        private UserServiceImpl userService;

        @PostMapping("/token")
        public ResponseEntity<Void> setToken(@RequestBody TokenRequestDTO tokenRequestDTO){
            userService.setToken(tokenRequestDTO);
            return ResponseEntity.ok().build();

        }

        @RequestMapping("/UserCreate")
        public ResponseEntity<Void> createUser(@RequestBody CreateUserRequestDTO createUserRequestDTO){
            userService.createUser(createUserRequestDTO);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }


}
