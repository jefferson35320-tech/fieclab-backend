package br.edu.fiec.FiecLab.Features.User.controller;

import br.edu.fiec.FiecLab.Features.User.Models.DTO.CreateUserRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.DTO.TokenRequestDTO;
import br.edu.fiec.FiecLab.Features.User.Models.DTO.UserDTO;
import br.edu.fiec.FiecLab.Features.User.Models.Entities.User;
import br.edu.fiec.FiecLab.Features.User.service.Impl.UserServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/users")
public class UserController {

        private UserServiceImpl userService;


        @PostMapping("/token")
        public ResponseEntity<Void> setToken(@RequestBody TokenRequestDTO tokenRequestDTO){
            userService.setToken(tokenRequestDTO);
            return ResponseEntity.ok().build();

        }

        @PostMapping("/UserCreate")
        public ResponseEntity<Void> createUser(@RequestBody CreateUserRequestDTO createUserRequestDTO){
            userService.createUser(createUserRequestDTO);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }


        @ResponseStatus(HttpStatus.OK)
        @DeleteMapping(value = "delete/{id}", produces = APPLICATION_JSON_VALUE)
        public void deleteUser(@RequestParam UUID id) {userService.deleteUserById(id); }

        @ResponseStatus(HttpStatus.OK)
        @GetMapping(value = "find/{id}", produces = APPLICATION_JSON_VALUE)
        public User findUserById(@RequestParam UUID id) { return userService.searchById(id); }

        @ResponseStatus(HttpStatus.OK)
        @GetMapping(value = "findAll", produces = APPLICATION_JSON_VALUE)
        public List<User> FindAllUsers() {
            return userService.FindAllUsers();
        }

        @ResponseStatus(HttpStatus.OK)
        @PutMapping(value = "update/{id}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
        public void update(@RequestParam UUID id, String token, @RequestBody UserDTO userDTO) {
            userService.UpdateUserById(id, userDTO, token); }

}
