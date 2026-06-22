package tn.esprit.backend_esprittech.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend_esprittech.dto.request.AssignChefRequest;
import tn.esprit.backend_esprittech.dto.request.CreateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.request.CreateUserRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateUserRequest;
import tn.esprit.backend_esprittech.dto.response.EquipeResponse;
import tn.esprit.backend_esprittech.dto.response.MessageResponse;
import tn.esprit.backend_esprittech.dto.response.PagedResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.mapper.EquipeMapper;
import tn.esprit.backend_esprittech.mapper.UserMapper;
import tn.esprit.backend_esprittech.service.EquipeRechercheService;
import tn.esprit.backend_esprittech.service.ReferentielService;
import tn.esprit.backend_esprittech.service.UserService;

/**
 * Endpoints réservés à l'administrateur : import du référentiel CSV,
 * création d'équipes et assignation des chefs.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration", description = "Import du référentiel et gestion des équipes (ADMIN)")
public class AdminController {

        private final ReferentielService referentielService;
        private final EquipeRechercheService equipeService;
        private final EquipeMapper equipeMapper;
        private final UserService userService;
        private final UserMapper userMapper;

        public AdminController(ReferentielService referentielService,
                        EquipeRechercheService equipeService,
                        EquipeMapper equipeMapper,
                        UserService userService,
                        UserMapper userMapper) {
                this.referentielService = referentielService;
                this.equipeService = equipeService;
                this.equipeMapper = equipeMapper;
                this.userService = userService;
                this.userMapper = userMapper;
        }

        @PostMapping(value = "/import-referentiel", consumes = "multipart/form-data")
        @Operation(summary = "Importer le référentiel", description = "Parse un fichier CSV (nom,prenom,email,identifiant,type,telephone) et alimente le référentiel.")
        public ResponseEntity<MessageResponse> importerReferentiel(@RequestParam("file") MultipartFile file) {
                int importes = referentielService.importerCsv(file);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(MessageResponse.of(importes + " utilisateur(s) importé(s) dans le référentiel"));
        }

        @PostMapping("/equipes")
        @Operation(summary = "Créer une équipe", description = "Crée une équipe de recherche et promeut le chef désigné au rôle CHEF_EQUIPE.")
        public ResponseEntity<EquipeResponse> creerEquipe(@Valid @RequestBody CreateEquipeRechercheRequest request) {
                EquipeResponse response = equipeService.create(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @PutMapping("/equipes/{id}/chef")
        @Operation(summary = "Assigner un chef", description = "Assigne (ou change) le chef d'une équipe existante. Le chef doit être un ENSEIGNANT.")
        public ResponseEntity<EquipeResponse> assignerChef(@PathVariable Long id,
                        @Valid @RequestBody AssignChefRequest request) {
                EquipeRecherche equipe = equipeService.assignerChef(id, request.chefId());
                EquipeResponse body = equipeMapper.toResponse(equipe, equipeService.compterMembres(equipe));
                return ResponseEntity.ok(body);
        }

        // ----- Gestion des utilisateurs -----

        @PostMapping("/users")
        @Operation(summary = "Créer un utilisateur", description = "Crée directement un compte (sans passer par le référentiel). Réservé à l'admin.")
        public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
                UserResponse body = userMapper.toResponse(userService.create(request));
                return ResponseEntity.status(HttpStatus.CREATED).body(body);
        }

        @GetMapping("/users")
        @Operation(summary = "Lister les utilisateurs", description = "Liste paginée des comptes, avec recherche libre (nom, prénom, email, identifiant).")
        public ResponseEntity<PagedResponse<UserResponse>> listerUsers(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String search) {
                Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
                Page<User> users = userService.list(search, pageable);
                return ResponseEntity.ok(PagedResponse.from(users, userMapper::toResponse));
        }

        @PutMapping("/users/{id}")
        @Operation(summary = "Modifier un utilisateur", description = "Met à jour l'identité, le rôle et l'état d'activation d'un utilisateur.")
        public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                        @Valid @RequestBody UpdateUserRequest request) {
                return ResponseEntity.ok(userMapper.toResponse(userService.update(id, request)));
        }

        @DeleteMapping("/users/{id}")
        @Operation(summary = "Supprimer un utilisateur", description = "Supprime un compte (un admin ne peut pas supprimer le sien).")
        public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long id) {
                userService.delete(id);
                return ResponseEntity.ok(MessageResponse.of("Utilisateur supprimé"));
        }
}
