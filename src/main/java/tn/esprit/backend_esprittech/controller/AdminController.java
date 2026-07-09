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

import tn.esprit.backend_esprittech.dto.request.CreateUserRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateUserRequest;
import tn.esprit.backend_esprittech.dto.response.MessageResponse;
import tn.esprit.backend_esprittech.dto.response.PagedResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.AdminCreateSujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetInvalidationRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.mapper.UserMapper;
import tn.esprit.backend_esprittech.service.ReferentielService;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetProjetService;

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
        private final UserService userService;
        private final UserMapper userMapper;
        private final SujetProjetService sujetProjetService;

        public AdminController(ReferentielService referentielService,
                        UserService userService,
                        UserMapper userMapper,
                        SujetProjetService sujetProjetService) {
                this.referentielService = referentielService;
                this.userService = userService;
                this.userMapper = userMapper;
                this.sujetProjetService = sujetProjetService;
        }

        @PostMapping(value = "/import-referentiel", consumes = "multipart/form-data")
        @Operation(summary = "Importer le référentiel", description = "Parse un fichier CSV (nom,prenom,email,identifiant,type,telephone) et alimente le référentiel.")
        public ResponseEntity<MessageResponse> importerReferentiel(@RequestParam("file") MultipartFile file) {
                int importes = referentielService.importerCsv(file);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(MessageResponse.of(importes + " utilisateur(s) importé(s) dans le référentiel"));
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

        // ----- Gestion des sujets -----

        @GetMapping("/encadrants")
        @Operation(summary = "Rechercher des encadrants affiliés", description = "Liste les enseignants et chefs d'équipe affiliés à une équipe, pour l'affectation d'un sujet.")
        public ResponseEntity<PagedResponse<UserResponse>> listerEncadrants(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "6") int size,
                        @RequestParam(required = false) String search) {
                Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "nom", "prenom"));
                Page<User> users = userService.listAffiliatedEncadrants(search, pageable);
                return ResponseEntity.ok(new PagedResponse<>(
                                users.map(userService::toEnrichedResponse).getContent(),
                                users.getNumber(),
                                users.getSize(),
                                users.getTotalElements(),
                                users.getTotalPages(),
                                users.isFirst(),
                                users.isLast()));
        }

        @GetMapping("/sujet-projets")
        @Operation(summary = "Lister les sujets", description = "Liste paginée de tous les sujets déposés, avec recherche et filtres.")
        public ResponseEntity<PagedResponse<SujetProjetResponse>> listerSujets(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "8") int size,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) CategorieSujet categorie,
                        @RequestParam(required = false) StatutSujet statut) {
                Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateCreation"));
                Page<SujetProjetResponse> sujets = sujetProjetService.listSujetsForAdmin(categorie, statut, search, pageable);
                return ResponseEntity.ok(new PagedResponse<>(
                                sujets.getContent(),
                                sujets.getNumber(),
                                sujets.getSize(),
                                sujets.getTotalElements(),
                                sujets.getTotalPages(),
                                sujets.isFirst(),
                                sujets.isLast()));
        }

        @GetMapping("/sujet-projets/demandes")
        @Operation(summary = "Lister les demandes en attente", description = "Demandes de sujets non encore traitées (soumis ou en attente).")
        public ResponseEntity<PagedResponse<SujetProjetResponse>> listerDemandes(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "8") int size,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) CategorieSujet categorie) {
                Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateCreation"));
                Page<SujetProjetResponse> demandes = sujetProjetService.listDemandesForAdmin(categorie, search, pageable);
                return ResponseEntity.ok(new PagedResponse<>(
                                demandes.getContent(),
                                demandes.getNumber(),
                                demandes.getSize(),
                                demandes.getTotalElements(),
                                demandes.getTotalPages(),
                                demandes.isFirst(),
                                demandes.isLast()));
        }

        @GetMapping("/sujet-projets/disponibles")
        @Operation(summary = "Lister les sujets disponibles", description = "Sujets publiés dans le catalogue (acceptés et visibles côté frontoffice).")
        public ResponseEntity<PagedResponse<SujetProjetResponse>> listerSujetsDisponibles(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "8") int size,
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) CategorieSujet categorie) {
                Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateCreation"));
                Page<SujetProjetResponse> sujets = sujetProjetService.listSujetsDisponiblesForAdmin(categorie, search, pageable);
                return ResponseEntity.ok(new PagedResponse<>(
                                sujets.getContent(),
                                sujets.getNumber(),
                                sujets.getSize(),
                                sujets.getTotalElements(),
                                sujets.getTotalPages(),
                                sujets.isFirst(),
                                sujets.isLast()));
        }

        @PostMapping("/sujet-projets")
        @Operation(summary = "Créer un sujet pour un encadrant", description = "Crée un sujet validé et publié au nom d'un enseignant affilié.")
        public ResponseEntity<SujetProjetResponse> creerSujet(
                        @Valid @RequestBody AdminCreateSujetProjetRequest request) {
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(sujetProjetService.creerSujetAsAdmin(request));
        }

        @PostMapping("/sujet-projets/{id}/valider")
        @Operation(summary = "Accepter une demande", description = "Valide un sujet en attente et l'ouvre aux candidatures.")
        public ResponseEntity<SujetProjetResponse> validerSujet(@PathVariable Long id) {
                return ResponseEntity.ok(sujetProjetService.validerSujetAsAdmin(id));
        }

        @PostMapping("/sujet-projets/{id}/invalider")
        @Operation(summary = "Refuser une demande", description = "Rejette un sujet en attente avec un motif enregistré.")
        public ResponseEntity<SujetProjetResponse> invaliderSujet(
                        @PathVariable Long id,
                        @Valid @RequestBody SujetInvalidationRequest request) {
                return ResponseEntity.ok(sujetProjetService.invaliderSujetAsAdmin(id, request));
        }

        @GetMapping("/sujet-projets/{id}")
        @Operation(summary = "Détail d'un sujet", description = "Retourne toutes les informations d'un sujet.")
        public ResponseEntity<SujetProjetResponse> getSujet(@PathVariable Long id) {
                return ResponseEntity.ok(sujetProjetService.getSujetForAdmin(id));
        }

        @PutMapping("/sujet-projets/{id}")
        @Operation(summary = "Modifier un sujet", description = "Met à jour le contenu d'un sujet déposé.")
        public ResponseEntity<SujetProjetResponse> modifierSujet(
                        @PathVariable Long id,
                        @Valid @RequestBody SujetProjetRequest request) {
                return ResponseEntity.ok(sujetProjetService.modifierSujetAsAdmin(id, request));
        }

        @DeleteMapping("/sujet-projets/{id}")
        @Operation(summary = "Supprimer un sujet", description = "Supprime définitivement un sujet.")
        public ResponseEntity<MessageResponse> supprimerSujet(@PathVariable Long id) {
                sujetProjetService.supprimerSujetAsAdmin(id);
                return ResponseEntity.ok(MessageResponse.of("Sujet supprimé"));
        }
}
