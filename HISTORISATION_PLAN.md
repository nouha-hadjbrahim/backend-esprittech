# Plan d'Historisation — Industrialisation Projets (MIGRATION TERMINÉE)

**Statut :** ✅ La grande majorité des méthodes métiers sont annotées avec `@Historise`. Aucun appel direct à `historiqueService.log()` ne subsiste dans les services migrés.  
**Dernière mise à jour :** Juillet 2026.  
**Remaining :** 3 méthodes identifiées non annotées.

## Résumé de la migration

| Étape | Statut |
|---|---|
| 1. Table `historique` (Flyway) | ✅ OK |
| 2. `ActionType` / `EntityType` / `Historique` entity | ✅ OK |
| 3. Annotation `@Historise` | ✅ OK |
| 4. Aspect AOP | ✅ OK |
| 5. `HistoriqueService` | ✅ OK |
| 6. `HistoriqueController` | ✅ OK |
| 7. Marquage de tous les services | ✅ OK |
| 8. Tests du système générique | ✅ OK (HistorisationIntegrationTest) |
| 9. Migration Industrialisation (suppression HistoriqueIndustrialisation) | ✅ OK |
| 10. `actorRole` passé de `String` à `@Enumerated(EnumType.STRING) Role` | ✅ OK |
| 11. `OVERRIDE` ajouté à `ActionType` | ✅ OK |

### Changements notables depuis le plan original

- **`actorRole`** : le champ était `String` dans `Historique.java`. Il a été changé en `@Enumerated(EnumType.STRING) Role` — la colonne DB reste `VARCHAR(30)`, aucune migration nécessaire.
- **`OVERRIDE`** ajouté à l'enum `ActionType` pour l'action `overrideLatestEvaluation` dans `EvaluationService`.
- **`newValuesExpression`** : propriété `String newValuesExpression() default ""` ajoutée à `@Historise` pour éviter la sérialisation automatique des DTO volumineux ou contenant des données sensibles.
- **`REJECT` → `REFUSE`** : Correction dans `EvaluationService` où `ActionType.REJECT` n'existe pas (c'était `REFUSE`).

---

## 1. Objectif

Mettre en place un système d'audit traçant **toutes les actions métier** (création, modification, validation, décision, suppression) sur l'ensemble des entités de l'application, avec **qui, quoi, quand, et les changements**.

---

## 2. Architecture — Table générique unique

| Champ | Type | Description |
|---|---|---|
| `id` | BIGINT (PK) | Auto-généré |
| `actor_id` | BIGINT (FK → users) | Qui a fait l'action |
| `actor_role` | VARCHAR(30) | Rôle au moment de l'action |
| `action` | VARCHAR(40) | Type d'action (CREATE, UPDATE, VALIDATE, etc.) |
| `entity_type` | VARCHAR(40) | Enum `EntityType` (ex: `SUJET_PROJET`, `CANDIDATURE`) |
| `entity_id` | BIGINT | ID de l'entité concernée |
| `summary` | TEXT | Résumé lisible : "Création du sujet 'Projet IA'" |
| `old_values` | JSON | Ancien état (UPDATE / DELETE) |
| `new_values` | JSON | Nouvel état (CREATE / UPDATE) |
| `metadata` | JSON | Infos supplémentaires (motif, commentaire, etc.) |
| `created_at` | DATETIME | Date de l'action |

---

## 3. Enums

### `ActionType`

```
CREATE, UPDATE, DELETE, VALIDATE, INVALIDATE, SUBMIT,
DECISION_GO, DECISION_NOGO, ACCEPT, REFUSE, ACTIVATE, DEACTIVATE,
REORDER, UPLOAD, LINK, RETRAIT, RETRAIT_ETUDIANT, DECLARER_TERMINAISON,
OUVRIR_CANDIDATURES, FERMER_CANDIDATURES,
ASSIGN_CHEF, RETIRER_CHEF, AJOUTER_MEMBRE, RETIRER_MEMBRE, DEMANDER_AFFILIATION,
TRAITER_AFFILIATION, RECALCULER_EVALUATION, OVERRIDE
```

### `EntityType`

Définit les types d'entités historisables. Stocké en base sous forme de `VARCHAR(40)` avec `@Enumerated(EnumType.STRING)`.

```
USER, SUJET_PROJET,
CANDIDATURE, PROJET_CATALOGUE, LIVRABLE, EQUIPE_RECHERCHE,
AFFILIATION_ENSEIGNANT, CRITERE_NOTE, CRITERE_ELIMINATOIRE,
CRITERE_NOTE_RULE, NOTE_LEVEL, EVALUATION,
CANDIDATURE_INDUSTRIALISATION
```

---

## 4. Implémentation technique

### 4.1 Annotation personnalisée `@Historise` — `annotation/Historise.java`

```java
@Target(METHOD)
@Retention(RUNTIME)
public @interface Historise {
    ActionType action() default ActionType.CREATE;
    String actionExpression() default "";                   // SpEL — dynamique (ex: #request.statut.name() == 'ACCEPTEE' ? 'ACCEPT' : 'REFUSE')
    EntityType entityType();                                // ← enum (type-safe)
    String entityIdExpression();                            // SpEL — ex: #result.id(), #id
    String summaryExpression();                             // SpEL — ex: 'Création ' + #result.libelle()
    String oldValuesExpression() default "";                 // SpEL — ex: @userService.getOldValues(#id)
    String newValuesExpression() default "";                 // SpEL — évite auto-sérialisation : {'actif': false}
    String metadataExpression() default "";                  // SpEL — ex: {'motif': #request.motif()}
}
```

### 4.2 Aspect AOP — `config/HistorisationAspect.java`

Un `@Aspect` Spring intercepte les méthodes annotées `@Historise` :

- Exécution de la méthode
- Après exécution : résout les expressions SpEL, capture le nouvel état et persiste l'`Historique`
- **Gestion d'erreur** : L'ensemble du code de logging est encapsulé dans un `try/catch (Exception e) { log.warn("Échec historisation {} {}", action, entityType, e); }` pour ne jamais casser la transaction métier

### 4.3 Service — `service/historique/HistoriqueService.java`

```java
@Service
public class HistoriqueService {
    void log(ActionType action, EntityType entityType, Long entityId, String summary,
             Map oldValues, Map newValues, Map metadata);
    PagedResponse<HistoriqueResponse> search(EntityType entityType, entityId, action, actorId, dateFrom, dateTo, pageable);
}
```

### 4.4 API REST — `controller/historique/HistoriqueController.java`

- `GET /api/historique` — liste paginée avec filtres
- `GET /api/historique/entity/{entityType}/{entityId}` — historique d'une entité (`entityType` = valeur de l'enum `EntityType`, ex: `CANDIDATURE_INDUSTRIALISATION`)
- `GET /api/historique/user/{userId}` — actions d'un utilisateur

---

## 5. Modules et actions à historiser

### Utilisateurs — ✅ Migré

| Service | Méthode | Action | oldValues | newValues |
|---|---|---|---|---|
| `UserService.create` | Création admin | CREATE (User) | — | auto-sérialisation |
| `UserService.update` | Modification | UPDATE (User) | `getOldValues` | auto-sérialisation |
| `UserService.delete` | Suppression | DELETE (User) | `getOldValues` | — |

### SujetProjet — ✅ Migré

| Service | Méthode | Action | oldValues | newValues |
|---|---|---|---|---|
| `SujetProjetService.creerSujet` | Création | CREATE | — | auto-sérialisation |
| `SujetProjetService.modifierSujet` | Modification | UPDATE | `getOldValues` | auto-sérialisation |
| `SujetProjetService.supprimerSujet` | Suppression | DELETE | `getOldValues` | — |
| `SujetProjetService.validerSujet` | Validation chef | VALIDATE | — | `newValuesExpression` |
| `SujetProjetService.invaliderSujet` | Refus chef | INVALIDATE | — | `newValuesExpression` |
| `SujetProjetService.validerSujetAsAdmin` | Validation admin | VALIDATE | — | `newValuesExpression` |
| `SujetProjetService.invaliderSujetAsAdmin` | Refus admin | INVALIDATE | — | `newValuesExpression` |
| `SujetProjetService.modifierSujetAsAdmin` | Modification admin | UPDATE | `getOldValues` | auto-sérialisation |
| `SujetProjetService.supprimerSujetAsAdmin` | Suppression admin | DELETE | `getOldValues` | — |
| `SujetProjetService.declarerTerminaison` | Terminaison | DECLARER_TERMINAISON | — | `newValuesExpression` |

### Candidature & Affectation — ✅ Migré (⚠️ 1 restante)

| Service | Méthode | Action | oldValues | newValues | metadata |
|---|---|---|---|---|---|
| `CandidatureService.ouvrirCandidatures` | Ouverture | OUVRIR_CANDIDATURES | — | — | — |
| `CandidatureService.deposerCandidature` | Dépôt | CREATE (Candidature) | — | auto-sérialisation | — |
| `CandidatureService.deciderCandidature` | Décision | dynamique via `actionExpression` | — | auto-sérialisation | motif |
| `CandidatureService.fermerCandidatures` | Fermeture | FERMER_CANDIDATURES | — | — | — |
| `CandidatureService.retirerMaCandidature` | Retrait étudiant | RETRAIT_ETUDIANT | `getOldValues` | — | — |
| `CandidatureService.retirerEtudiant` | Retrait encadrant | RETRAIT | `getOldValues` | — | motif |
| `CandidatureService.declarerTerminaison` | Terminaison (dupliqué) | ❌ non annoté (même action que `SujetProjetService.declarerTerminaison`) | — | — | — |

### ProjetCatalogue — ✅ Migré

| Service | Méthode | Action | oldValues | newValues | metadata |
|---|---|---|---|---|---|
| `ProjetCatalogueService.creerProjet` | Création | CREATE | — | auto-sérialisation | — |
| `ProjetCatalogueService.valider` | Validation chef | VALIDATE | — | auto-sérialisation | — |
| `ProjetCatalogueService.refuser` | Refus chef | REFUSE | — | auto-sérialisation | motifRefus (`metadataExpression`) |

### Livrable — ✅ Migré (reorder ajouté dernier)

| Service | Méthode | Action | oldValues | newValues |
|---|---|---|---|---|
| `LivrableService.upload` | Upload fichier | UPLOAD | — | auto-sérialisation |
| `LivrableService.addLink` | Ajout lien | CREATE (Livrable) | — | auto-sérialisation |
| `LivrableService.update` | Modification | UPDATE (Livrable) | `getOldValues` | auto-sérialisation |
| `LivrableService.delete` | Suppression logique | DELETE | `getOldValues` | `{'actif': false}` |

### Équipes — ✅ Migré

| Service | Méthode | Action | oldValues | newValues |
|---|---|---|---|---|
| `EquipeRechercheService.create` | Création | CREATE (EquipeRecherche) | — | auto-sérialisation |
| `EquipeRechercheService.update` | Modification | UPDATE | `getOldValues` | auto-sérialisation |
| `EquipeRechercheService.assignerChef` | Assignation chef | ASSIGN_CHEF | `getOldValues` | auto-sérialisation |
| `EquipeRechercheService.ajouterMembres` | Ajout membres | AJOUTER_MEMBRE | `getOldValues` | auto-sérialisation |
| `EquipeRechercheService.retirerMembre` | Retrait membre | RETIRER_MEMBRE | `getOldValues` | auto-sérialisation |
| `EquipeRechercheService.retirerChef` | Retrait chef | RETIRER_CHEF | `getOldValues` | auto-sérialisation |
| `EquipeRechercheService.delete` | Suppression | DELETE | `getOldValues` | — |

### Affiliations — ✅ Migré

| Service | Méthode | Action | oldValues |
|---|---|---|---|
| `AffiliationEnseignantService.demander` | Demande | DEMANDER_AFFILIATION | `getOldValues` |
| `AffiliationEnseignantService.traiter` | Décision | TRAITER_AFFILIATION | `getOldValues` |

### Critères d'évaluation — ✅ Migré

| Service | Méthode | Action | oldValues | newValues |
|---|---|---|---|---|
| `CritereNoteService.create` | Création | CREATE (CritereNote) | — | auto-sérialisation |
| `CritereNoteService.update` | Modification | UPDATE | `getOldValues` | auto-sérialisation |
| `CritereNoteService.delete` | Suppression | DELETE | `getOldValues` | — |
| `CritereNoteService.activate` | Activation | ACTIVATE | `getOldValues` | `{'actif': true}` |
| `CritereNoteService.deactivate` | Désactivation | DEACTIVATE | `getOldValues` | `{'actif': false}` |
| `CritereNoteService.reorder` | Réordonnancement | REORDER | `getOldValues(#requests[0].critereId())` | — |
| `CritereEliminatoireService.create` | Création | CREATE (CritereEliminatoire) | — | auto-sérialisation |
| `CritereEliminatoireService.update` | Modification | UPDATE | `getOldValues` | auto-sérialisation |
| `CritereEliminatoireService.activate` | Activation | ACTIVATE | `getOldValues` | `{'actif': true}` |
| `CritereEliminatoireService.deactivate` | Désactivation | DEACTIVATE | `getOldValues` | `{'actif': false}` |
| `CritereEliminatoireService.reorder` | Réordonnancement | REORDER | `getOldValues(#requests[0].critereId())` | — |
| `CritereNoteRuleService.*` | CRUD règles | CREATE / UPDATE / DELETE | `getOldValues` | auto-sérialisation |
| `NoteLevelService.*` | CRUD niveaux | CREATE / UPDATE / DELETE | `getOldValues` | auto-sérialisation |

### Évaluation — ✅ Migré

| Service | Méthode | Action | oldValues | newValues |
|---|---|---|---|---|
| `EvaluationService.calculerEvaluation` | Évaluation manuelle | CREATE (Evaluation) | — | `newValuesExpression` (évite 44 champs) |
| `EvaluationService.calculateAutomaticEvaluation` | Recalcul | RECALCULER_EVALUATION | — | `newValuesExpression` |
| `EvaluationService.validateLatestEvaluation` | Validation | VALIDATE | `getOldValues` | `newValuesExpression` |
| `EvaluationService.rejectLatestEvaluation` | Rejet | REFUSE | `getOldValues` | `newValuesExpression` |
| `EvaluationService.overrideLatestEvaluation` | Override | OVERRIDE | `getOldValues` | `newValuesExpression` |
| `EvaluationService.calculateForSystem` | Sys interne | — | — | non annoté (interne) |

### Industrialisation — ⚠️ Migré (2 méthodes restantes)

⚠️ **Migration depuis l'ancien `HistoriqueIndustrialisation`** : L'entité spécifique `HistoriqueIndustrialisation` (table `historique_industrialisation`) est **supprimée** et remplacée par le système générique `Historique`. Le service `IndustrialisationService` utilise `@Historise` avec `newValuesExpression` pour éviter la boucle de sérialisation sur `CandidatureIndustrialisationResponse`. Voir section ci-dessous pour le mapping détaillé.

| Méthode | Action | oldValues | newValues |
|---|---|---|---|
| `IndustrialisationService.create()` | CREATE | — | `newValuesExpression` (évite `CandidatureIndustrialisationResponse.historique` → cycle) |
| `IndustrialisationService.soumettre()` | SUBMIT | `getOldValues` | `newValuesExpression` |
| `IndustrialisationService.decideGo()` | DECISION_GO | `getOldValues` | `newValuesExpression` |
| `IndustrialisationService.decideNoGo()` | DECISION_NOGO | `getOldValues` | `newValuesExpression` |
| `IndustrialisationService.saveReponses()` | Sauvegarde réponses | ❌ non annoté | — |
| `IndustrialisationService.uploadPreuve()` | Upload preuve | ❌ non annoté | — |


`entityType` = `EntityType.CANDIDATURE_INDUSTRIALISATION` dans tous les appels.

| Méthode | `action` | `oldValues` | `newValues` | `metadata` | `summary` |
|---|---|---|---|---|---|
| `create()` | `CREATE` | `null` | `{"statut":"BROUILLON","type":"..."}` | `{"commentaire":"..."}` (si présent) | `"Création de la demande d'industrialisation"` |
| `soumettre()` | `SUBMIT` | `{"statut":"BROUILLON"}` | `{"statut":"SOUMISE"}` | `null` | `"Soumission de la demande à la CI"` |
| `decideGo()` | `DECISION_GO` | `{"statut":"SOUMISE"}` | `{"statut":"GO","orientation":"DSI"}` | `{"commentaire":"..."}` | `"Décision GO - DSI"` |
| `decideNoGo()` | `DECISION_NOGO` | `{"statut":"RECEVABLE"}` | `{"statut":"NO_GO"}` | `{"motif":"..."}` | `"Décision NO GO"` |

---

## 6. Fichiers créés / modifiés / supprimés

### Créés

```
src/main/java/.../enums/ActionType.java
src/main/java/.../enums/EntityType.java            ← type-safe pour entityType
src/main/java/.../annotation/Historise.java
src/main/java/.../config/HistorisationAspect.java
src/main/java/.../entity/historique/Historique.java
src/main/java/.../repository/historique/HistoriqueRepository.java
src/main/java/.../service/historique/HistoriqueService.java
src/main/java/.../controller/historique/HistoriqueController.java
src/main/java/.../dto/response/HistoriqueResponse.java
```

### Modifiés

- `.../entity/industrialisation/CandidatureIndustrialisation.java` — Retirer `@OneToMany historique`
- `.../service/industrialisation/IndustrialisationService.java` — Injecter `HistoriqueService`, supprimer `HistoriqueIndustrialisationRepository`, remplacer `addHistory()` par `historiqueService.log()`
- `.../dto/industrialisation/HistoriqueIndustrialisationResponse.java` — Ajouter `static from(HistoriqueResponse)`

### Supprimés

- `.../entity/industrialisation/HistoriqueIndustrialisation.java`
- `.../repository/industrialisation/HistoriqueIndustrialisationRepository.java`

## 7. Migration DB (Flyway)

```sql
CREATE TABLE historique (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT NOT NULL,
    actor_role VARCHAR(30) NOT NULL,
    action VARCHAR(40) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id BIGINT,
    summary TEXT NOT NULL,
    old_values JSON,
    new_values JSON,
    metadata JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_historique_entity (entity_type, entity_id),
    INDEX idx_historique_actor (actor_id),
    INDEX idx_historique_actor_date (actor_id, created_at DESC),
    INDEX idx_historique_action (action),
    INDEX idx_historique_created (created_at),
    FOREIGN KEY (actor_id) REFERENCES users(id)
);
```

## 8. Étapes d'implémentation — Statut final

| Étape | Statut |
|---|---|
| 1. **Migration DB** — Créer la table `historique` | ✅ |
| 2. **Enum + Entity + Repository** — `ActionType`, `Historique`, `HistoriqueRepository` | ✅ |
| 3. **Annotation `@Historise`** — Définir l'annotation avec SpEL | ✅ (`newValuesExpression` + `metadataExpression` ajoutés) |
| 4. **Aspect AOP** — Implémenter `HistorisationAspect` | ✅ |
| 5. **Service** — `HistoriqueService` avec recherche paginée | ✅ |
| 6. **Controller** — `HistoriqueController` avec endpoints REST | ✅ |
| 7. **Marquage des services** — Ajouter `@Historise` sur toutes les méthodes | ⚠️ (~77 méthodes, 3 restantes : `CandidatureService.declarerTerminaison`, `IndustrialisationService.saveReponses`, `uploadPreuve`) |
| 8. **Tests du système générique** — `HistorisationIntegrationTest` | ✅ |
| 9. **Migration Industrialisation** — Supprimer `HistoriqueIndustrialisation` | ✅ |
| 10. **Type-safety `actorRole`** — Changer `String` → `@Enumerated(EnumType.STRING) Role` | ✅ |
| 11. **Ajout `OVERRIDE`** à `ActionType` | ✅ |

---

## 9. Capture des `oldValues` et `newValues` — Comparaison des approches

### Problème

L'aspect `@Historise` ne peut pas capturer automatiquement les `oldValues` (état avant modification) car il ne connaît pas les repositories JPA. Les `newValues` sont sérialisées depuis la valeur de retour après `proceed()`, mais l'état préexistant en base est inaccessible sans une requête explicite.

### Approches possibles

| Approche | Mécanisme | Problèmes |
|---|---|---|
| **A — SpEL direct** | `oldValuesExpression = "@equipeRechercheRepository.findById(#id).orElse(null)"` | **ClassCastException** (entité → `Map`), **LazyInitializationException** (hors transaction), cycles JPA / proxies Hibernate, couplage au nom du bean, pas de vérification à la compilation |
| **B — Appel direct** | `historiqueService.log(UPDATE, ..., oldValues, newValues, ...)` dans le service | **Boilerplate**, oubli facile, résolution manuelle de l'acteur |
| **C — Méthode utilitaire `getOldValues()`** (✓ retenue) | `oldValuesExpression = "@equipeXxxService.getOldValues(#id)"` avec une méthode dédiée retournant `Map<String, Object>` | Une méthode utilitaire par service, mais : type-safe, transaction disponible, pas de proxy Hibernate, pas de `ClassCastException` |

### Approche C — Détail

Chaque service expose une méthode publique `getOldValues(id)` qui :

1. Récupère l'entité en base (dans une transaction de lecture)
2. Extrait **uniquement les champs pertinents** dans une `Map<String, Object>` (pas de lazy loading)
3. Retourne la Map — consommable directement par l'aspect

```java
public Map<String, Object> getOldValues(Long id) {
    EquipeRecherche e = getEntite(id);
    Map<String, Object> map = new HashMap<>();
    map.put("nom", e.getNom());
    map.put("chefId", e.getChef() != null ? e.getChef().getId() : null);
    map.put("memberIds", e.getMembers() != null
            ? e.getMembers().stream().map(User::getId).toList()
            : List.of());
    return map;
}
```

Dans l'annotation :
```java
@Historise(
    action = UPDATE,
    entityType = EQUIPE_RECHERCHE,
    entityIdExpression = "#id",
    oldValuesExpression = "@equipeRechercheService.getOldValues(#id)",
    summaryExpression = "'Modification...'"
)
```

L'aspect évalue `oldValuesExpression` **avant** `proceed()` (état encore intact), puis évalue `metadataExpression` et `newValues` **après** `proceed()`.

### Capture des `newValues` — `newValuesExpression`

Par défaut, l'aspect sérialise automatiquement la valeur de retour (`#result`) en JSON pour les actions `CREATE` et `UPDATE`. Ce comportement est pratique mais peut poser problème dans certains cas :

| Problème | Exemple | Solution |
|---|---|---|
| DTO trop volumineux | `SujetProjetResponse` (30+ champs), `EvaluationResponse` (44 champs) | `newValuesExpression = "{'statut': #result.statut(), 'message': '...'}"` |
| Données sensibles | `AuthResponse` contient un JWT | `newValuesExpression = "{'email': #result.email()}"` |
| Méthode `void` (DELETE, ACTIVATE, etc.) | `delete()` ne retourne rien | `newValuesExpression = "{'actif': false}"` |
| Cycle de sérialisation | `CandidatureIndustrialisationResponse` contient une liste d'`Historique` | `newValuesExpression = "{'statut': ..., 'type': ...}"` |

L'expression SpEL est évaluée **après** `proceed()` et doit produire une `Map<String, Object>`.

```java
@Historise(
    action = DEACTIVATE,
    entityType = CRITERE_NOTE,
    entityIdExpression = "#id",
    oldValuesExpression = "@critereNoteService.getOldValues(#id)",
    newValuesExpression = "{'actif': false}",
    summaryExpression = "'Désactivation critère note (id=' + #id + ')'"
)
public void deactivate(Long id) { ... }
```

### Quand utiliser quoi

| Cas | Solution |
|---|---|
| `CREATE` | `@Historise` simple — `oldValues` = `null` (rien n'existait avant) |
| `UPDATE` / `DELETE` avec état capturable | `@Historise` + `oldValuesExpression` → service.`getOldValues()` |
| `UPDATE` / `DELETE` sans état pertinent | `@Historise` simple — `oldValues` = `null` |
| DTO volumineux ou sensible | Ajouter `newValuesExpression` |
| Méthode `void` avec état connu | Ajouter `newValuesExpression` |
| Bulk operation (`reorder`) | `@Historise` avec `entityIdExpression = "#requests[0].critereId()"` — logue le premier élément |
| Log complexe (oldValues construit à la main, décision dynamique) | Appel direct `historiqueService.log()` (déprécié — plus aucun service ne l'utilise) |

## 10. Points d'attention

- **Transactions** : L'aspect log **immédiatement après** `proceed()` (dans la même transaction). L'historique est donc visible immédiatement, mais rollbacké si la transaction métier échoue.
- **JSON old/new values** : Utiliser Jackson (`ObjectMapper`) pour sérialiser les Maps
- **Extraction SpEL** : Utiliser `SpelExpressionParser` pour résoudre `entityIdExpression`, `summaryExpression`, `oldValuesExpression`, `newValuesExpression`, `metadataExpression` et `actionExpression` sur le contexte d'exécution
- **Performance** : L'écriture des logs est synchrone mais protégée par un `try/catch (Exception)` — ne casse jamais la transaction métier, même en cas d'échec de l'historisation
- **Pagination** : L'API de consultation supporte le filtrage par entité, action, acteur, période
- **Rétrocompatibilité API** : Le DTO `HistoriqueIndustrialisationResponse` est conservé pour ne pas casser l'API. La méthode statique `from(HistoriqueResponse)` utilise Jackson `ObjectMapper` pour parser les JSON `oldValues`/`newValues`/`metadata` et reconstruire les champs `statutSource`, `statutCible`, `motif`, `commentaire`
- **Type safety** : `entityType` est un enum `EntityType` (et non un `String`) — pas de risque de fautes de frappe, auto-complétion dans l'IDE
- **`actorRole`** : Changé de `String` à `@Enumerated(EnumType.STRING) Role`. La colonne DB reste `VARCHAR(30)`, aucune migration nécessaire.
- **`OVERRIDE`** : Ajouté à `ActionType` pour `EvaluationService.overrideLatestEvaluation`
- **`REJECT` vs `REFUSE`** : L'enum `ActionType` contient `REFUSE` et non `REJECT`. L'ancien code utilisant `REJECT` a été corrigé.
- **SpEL avec paramètres liste** : Pour les `reorder`, l'expression `#requests[0].critereId()` accède au premier élément de la liste de paramètres. Fonctionne car `MethodBasedEvaluationContext` expose les paramètres par nom.
