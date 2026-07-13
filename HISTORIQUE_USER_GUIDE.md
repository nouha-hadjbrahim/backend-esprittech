# Guide d'utilisation — Système d'Historisation

**Public :** Développeurs, chefs d'équipe, utilisateurs métier  
**Niveau :** Débutant — aucune connaissance pré-requise du système  
**Dernière mise à jour :** Juillet 2026

---

## 1. C'est quoi l'historisation ?

### En une phrase

Le système d'historisation enregistre automatiquement **toutes les actions importantes** faites dans l'application : qui a créé quoi, qui a modifié quoi, qui a validé ou refusé quoi, et à quel moment.

### Pourquoi c'est utile ?

- **Audit** : savoir exactement ce qui s'est passé sur un projet ou une candidature
- **Traçabilité** : comprendre pourquoi une décision a été prise (motif, commentaire)
- **Debug** : retrouver qui a modifié une donnée et quand
- **Conformité** : répondre aux exigences de traçabilité des actions

### Comment ça marche ? (simplifié)

```
✦ Un développeur met une étiquette @Historise sur une méthode
✦ À chaque appel de cette méthode, le système écrit automatiquement
  dans la table "historique" :
    • Qui a fait l'action       (ex: Karim Mokaddem)
    • Quelle action             (ex: VALIDATE)
    • Sur quelle entité         (ex: SujetProjet #42)
    • Résumé                    (ex: "Validation du sujet 'Projet IA'")
    • Avant/après (old/new)     (ex: statut passé de "BROUILLON" à "VALIDE")
    • Date                       (ex: 05/07/2026 à 14:30)
```

**Important :** Vous n'avez rien à configurer. Les développeurs ont déjà annoté ~44 méthodes. Vous pouvez dès maintenant consulter l'historique via l'API.

---

## 2. Consulter l'historique (API)

### 2.1 Pré-requis

- Être connecté avec un compte ayant le rôle **ADMIN**, **CI** ou **CHEF_EQUIPE**
- Récupérer un token JWT via `POST /api/auth/login`

### 2.2 Endpoint 1 : Recherche avec filtres

```
GET /api/historique
```

C'est le endpoint principal. Vous pouvez filtrer par entité, action, acteur, période.

**Exemple simple — les 20 dernières actions :**
```bash
curl -X GET 'http://localhost:8080/api/historique' \
  -H 'Authorization: Bearer <votre-token-jwt>'
```

**Exemple — historique d'un sujet de projet :**
```bash
curl -X GET 'http://localhost:8080/api/historique?entityType=SUJET_PROJET&entityId=42' \
  -H 'Authorization: Bearer <votre-token-jwt>'
```

**Exemple — toutes les validations faites par l'utilisateur #3 depuis juin 2026 :**
```bash
curl -X GET 'http://localhost:8080/api/historique?action=VALIDATE&actorId=3&dateFrom=2026-06-01T00:00:00' \
  -H 'Authorization: Bearer <votre-token-jwt>'
```

**Tous les paramètres disponibles :**

| Paramètre | Exemple | À quoi ça sert |
|-----------|---------|----------------|
| `entityType` | `SUJET_PROJET` | Type d'entité (voir annexe B) |
| `entityId` | `42` | ID de l'entité |
| `action` | `VALIDATE` | Type d'action (voir annexe A) |
| `actorId` | `3` | ID de l'utilisateur |
| `dateFrom` | `2026-06-01T00:00:00` | Début de période |
| `dateTo` | `2026-07-01T00:00:00` | Fin de période |
| `page` | `0` | Numéro de page (défaut: 0) |
| `size` | `10` | Éléments par page (défaut: 20) |

### 2.3 Endpoint 2 : Historique d'une entité spécifique

```
GET /api/historique/entity/{entityType}/{entityId}
```

Raccourci pratique pour voir tout l'historique d'une entité sans paramètres supplémentaires.

```bash
curl -X GET 'http://localhost:8080/api/historique/entity/CANDIDATURE_INDUSTRIALISATION/15' \
  -H 'Authorization: Bearer <votre-token-jwt>'
```

### 2.4 Endpoint 3 : Actions d'un utilisateur

```
GET /api/historique/user/{userId}
```

```bash
curl -X GET 'http://localhost:8080/api/historique/user/5' \
  -H 'Authorization: Bearer <votre-token-jwt>'
```

### 2.5 Réponse API — que signifient les champs ?

```json
{
  "content": [
    {
      "id": 1,
      "actorId": 1,
      "actorNom": "Mokaddem",
      "actorPrenom": "Karim",
      "actorRole": "ADMIN",
      "action": "CREATE",
      "entityType": "SUJET_PROJET",
      "entityId": 42,
      "summary": "Création du sujet 'Projet IA'",
      "oldValues": null,
      "newValues": "{\"id\":42,\"titre\":\"Projet IA\",\"statut\":\"BROUILLON\"}",
      "metadata": null,
      "createdAt": "2026-07-05T14:30:00"
    },
    {
      "id": 2,
      "actorId": 1,
      "actorNom": "Mokaddem",
      "actorPrenom": "Karim",
      "actorRole": "ADMIN",
      "action": "UPDATE",
      "entityType": "SUJET_PROJET",
      "entityId": 42,
      "summary": "Modification du sujet #42",
      "oldValues": "{\"titre\":\"Projet IA\",\"description\":\"Ancienne description\"}",
      "newValues": "{\"titre\":\"Projet IA\",\"description\":\"Nouvelle description\"}",
      "metadata": null,
      "createdAt": "2026-07-05T15:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

| Champ | Signification |
|-------|---------------|
| `actorId`, `actorNom`, `actorPrenom` | Qui a fait l'action |
| `actorRole` | Rôle de la personne au moment de l'action |
| `action` | Type d'action (CREATE, UPDATE, VALIDATE, etc.) |
| `entityType` | Type d'entité concernée |
| `entityId` | ID de l'entité concernée |
| `summary` | Résumé en français, facile à lire |
| `oldValues` | Avant la modification (JSON) — `null` si création |
| `newValues` | Après la modification (JSON) — `null` si suppression |
| `metadata` | Infos supplémentaires (motif de refus, commentaire, etc.) |
| `createdAt` | Date et heure de l'action |

---

## 3. Guide du développeur : ajouter `@Historise`

Cette section est pour les **développeurs** qui veulent ajouter l'historisation à une nouvelle méthode.

### 3.1 Cas simple : Création d'entité

```java
@Historise(
    action = ActionType.CREATE,
    entityType = EntityType.MA_NOUVELLE_ENTITE,
    entityIdExpression = "#result.id()",
    summaryExpression = "'Création de ' + #result.libelle()"
)
public MaNouvelleEntiteResponse creer(CreerRequest request) {
    // ...
}
```

**Ce qui se passe :**
- `entityIdExpression = "#result.id()"` → récupère l'ID depuis la valeur de retour
- `summaryExpression` → construit un résumé lisible
- `newValues` → auto-sérialisé depuis `#result` (DTO complet)
- `oldValues` → null (rien n'existait avant)

### 3.2 Avec oldValues : Modification ou suppression

Pour capturer l'état **avant** modification, ajoutez une méthode utilitaire dans votre service :

```java
public Map<String, Object> getOldValues(Long id) {
    MaEntite e = repository.findById(id).orElse(null);
    if (e == null) return null;
    Map<String, Object> map = new HashMap<>();
    map.put("titre", e.getTitre());
    map.put("statut", e.getStatut());
    // N'ajoutez que les champs pertinents — pas de lazy loading
    return map;
}
```

Puis référencez-la dans l'annotation :

```java
@Historise(
    action = ActionType.UPDATE,
    entityType = EntityType.MA_NOUVELLE_ENTITE,
    entityIdExpression = "#id",
    oldValuesExpression = "@monService.getOldValues(#id)",
    summaryExpression = "'Modification de l entité #' + #id"
)
public MaNouvelleEntiteResponse modifier(Long id, ModifierRequest request) {
    // ...
}
```

### 3.3 Éviter l'auto-sérialisation : newValuesExpression

Parfois, le DTO retourné est trop volumineux (ex: 44 champs) ou contient des données sensibles (ex: JWT). Utilisez `newValuesExpression` :

```java
@Historise(
    action = ActionType.CREATE,
    entityType = EntityType.MA_NOUVELLE_ENTITE,
    entityIdExpression = "#result.id()",
    newValuesExpression = "{'titre': #result.titre(), 'statut': #result.statut()}",
    summaryExpression = "'Création de ' + #result.titre()"
)
public MaNouvelleEntiteResponse creer(CreerRequest request) {
    // ...
}
```

**Syntaxe :** les accolades `{}` avec des paires clé-valeur en quotes simples.

### 3.4 Méthode void (DELETE, ACTIVATE, etc.)

Quand la méthode ne retourne rien (`void`), l'auto-sérialisation ne peut pas fonctionner. Fournissez `newValuesExpression` explicitement :

```java
@Historise(
    action = ActionType.DEACTIVATE,
    entityType = EntityType.MA_NOUVELLE_ENTITE,
    entityIdExpression = "#id",
    oldValuesExpression = "@monService.getOldValues(#id)",
    newValuesExpression = "{'actif': false}",
    summaryExpression = "'Désactivation de l entité #' + #id"
)
public void deactiver(Long id) {
    // ...
}
```

### 3.5 Action dynamique (SpEL conditionnel)

Si l'action dépend d'une condition métier :

```java
@Historise(
    actionExpression = "#request.statut().name() == 'ACCEPTEE' ? 'ACCEPT' : 'REFUSE'",
    entityType = EntityType.MA_NOUVELLE_ENTITE,
    entityIdExpression = "#id",
    summaryExpression = "Traitement de la candidature #" + #id",
    metadataExpression = "{'motif': #request.motif()}"
)
public void traiter(Long id, TraiterRequest request) {
    // ...
}
```

### 3.6 Anneau complet : toutes les expressions SpEL disponibles

| Variable | Source | Exemple |
|----------|--------|---------|
| `#id`, `#request`, etc. | Paramètres de la méthode | `#request.motif()` |
| `#result` | Valeur de retour de la méthode | `#result.id()`, `#result.titre()` |
| `@nomBean` | Bean Spring injecté | `@monService.getOldValues(#id)` |
| `#requests[0].critereId()` | Accès au premier élément d'une liste | Pour les `reorder` |

---

## 4. Exemples concrets

### Exemple 1 : Création d'un sujet de projet

**Code :**
```java
@Historise(
    action = ActionType.CREATE,
    entityType = EntityType.SUJET_PROJET,
    entityIdExpression = "#result.id()",
    summaryExpression = "'Création du sujet \'' + #result.libelle() + '\''"
)
public SujetProjetResponse creerSujet(CreerSujetRequest request) { ... }
```

**Entrée en base :**
| Champ | Valeur |
|-------|--------|
| `action` | `CREATE` |
| `entity_type` | `SUJET_PROJET` |
| `entity_id` | 42 |
| `summary` | `Création du sujet 'Projet IA'` |
| `old_values` | `null` |
| `new_values` | `{"id":42,"titre":"Projet IA","description":"...","statut":"BROUILLON"}` |

### Exemple 2 : Décision GO sur une candidature industrialisation

**Code :**
```java
@Historise(
    action = ActionType.DECISION_GO,
    entityType = EntityType.CANDIDATURE_INDUSTRIALISATION,
    entityIdExpression = "#id",
    oldValuesExpression = "@industrialisationService.getOldValues(#id)",
    newValuesExpression = "{'statut': #result.statut(), 'orientation': #result.orientation()}",
    summaryExpression = "'Décision GO - ' + #result.orientation()",
    metadataExpression = "{'commentaire': #request.commentaire()}"
)
public CandidatureIndustrialisationResponse decideGo(Long id, DecisionRequest request) { ... }
```

**Entrée en base :**
| Champ | Valeur |
|-------|--------|
| `action` | `DECISION_GO` |
| `entity_type` | `CANDIDATURE_INDUSTRIALISATION` |
| `entity_id` | 15 |
| `summary` | `Décision GO - DSI` |
| `old_values` | `{"statut":"SOUMISE"}` |
| `new_values` | `{"statut":"GO","orientation":"DSI"}` |
| `metadata` | `{"commentaire":"Projet validé par la CI"}` |

---

## 5. Tester l'historisation

### 5.1 Lancer les tests existants

```bash
# Tous les tests
./mvnw.cmd clean test

# Uniquement les tests d'historisation
./mvnw.cmd clean test -Dtest=HistorisationIntegrationTest,HistoriqueServiceTest
```

### 5.2 Vérifier manuellement (via API)

```bash
# 1. Connectez-vous en tant qu'admin
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@esprittech.tn","password":"Admin@1234"}' \
  | jq -r '.accessToken')

# 2. Faites une action (ex: créez un sujet de projet) ...

# 3. Vérifiez l'historique
curl -X GET 'http://localhost:8080/api/historique?entityType=SUJET_PROJET&size=5' \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 5.3 Vérifier directement en base (MySQL)

```sql
SELECT h.id, u.email, h.action, h.entity_type, h.entity_id, h.summary, h.created_at
FROM historique h
JOIN users u ON u.id = h.actor_id
WHERE h.entity_type = 'SUJET_PROJET' AND h.entity_id = 42
ORDER BY h.created_at DESC;
```

---

## 6. Annexe A : Liste des actions (ActionType)

Toutes les actions possibles, avec leur signification :

| Action | Signification | Exemple d'utilisation |
|--------|---------------|----------------------|
| `CREATE` | Création d'une entité | Nouveau sujet, nouvelle candidature |
| `UPDATE` | Modification d'une entité | Modification d'un sujet, d'un livrable |
| `DELETE` | Suppression d'une entité | Suppression d'un critère, d'un domaine |
| `VALIDATE` | Validation | Validation d'un sujet par le chef |
| `INVALIDATE` | Invalidation / Refus | Refus d'un sujet par le chef |
| `SUBMIT` | Soumission | Soumission d'une candidature industrialisation |
| `DECISION_GO` | Décision GO | Feu vert pour industrialisation |
| `DECISION_NOGO` | Décision NO GO | Refus d'industrialisation |
| `ACCEPT` | Acceptation | Acceptation d'une candidature sujet |
| `REFUSE` | Refus | Refus d'une candidature sujet |
| `ACTIVATE` | Activation | Activation d'un critère |
| `DEACTIVATE` | Désactivation | Désactivation d'un critère |
| `REORDER` | Réordonnancement | Changement d'ordre des critères |
| `UPLOAD` | Upload de fichier | Upload d'un livrable |
| `LINK` | Ajout de lien | Ajout d'un lien vers un livrable |
| `RETRAIT` | Retrait (encadrant) | Retrait d'un étudiant par l'encadrant |
| `RETRAIT_ETUDIANT` | Retrait (étudiant) | Retrait volontaire d'un étudiant |
| `DECLARER_TERMINAISON` | Déclaration de terminaison | Projet marqué comme terminé |
| `OUVRIR_CANDIDATURES` | Ouverture des candidatures | Période de candidatures ouverte |
| `FERMER_CANDIDATURES` | Fermeture des candidatures | Période de candidatures fermée |

| `ASSIGN_CHEF` | Assignation chef d'équipe | Promotion d'un membre comme chef |
| `RETIRER_CHEF` | Retrait chef d'équipe | Chef remplacé |
| `AJOUTER_MEMBRE` | Ajout membre équipe | Nouveau membre dans une équipe |
| `RETIRER_MEMBRE` | Retrait membre équipe | Membre retiré d'une équipe |
| `DEMANDER_AFFILIATION` | Demande d'affiliation | Enseignant demande affiliation |
| `TRAITER_AFFILIATION` | Traitement d'affiliation | Acceptation/refus d'affiliation |
| `RECALCULER_EVALUATION` | Recalcul évaluation | Recalcul automatique ou manuel |
| `OVERRIDE` | Surcharge évaluation | Forçage d'une note d'évaluation |

---

## 7. Annexe B : Liste des entités (EntityType)

| Entité | Description |
|--------|-------------|
| `USER` | Utilisateur (compte) |
| `SUJET_PROJET` | Sujet de projet (PFE/Stage) |
| `CANDIDATURE` | Candidature à un sujet |
| `PROJET_CATALOGUE` | Projet catalogue |
| `LIVRABLE` | Livrable (fichier ou lien) |
| `EQUIPE_RECHERCHE` | Équipe de recherche |
| `AFFILIATION_ENSEIGNANT` | Affiliation d'un enseignant à une équipe |
| `EVALUATION` | Évaluation d'un projet |
| `CANDIDATURE_INDUSTRIALISATION` | Candidature à l'industrialisation |

---

## 8. Questions fréquentes

**Q : Je consulte l'historique mais il est vide. Pourquoi ?**

R : Vérifiez d'abord que vous avez bien un token valide et le bon rôle (ADMIN, CI ou CHEF_EQUIPE). Ensuite, assurez-vous que des actions ont été faites après le déploiement du système d'historisation. Les données avant la migration ne sont pas dans la nouvelle table.

**Q : Puis-je modifier ou supprimer une entrée d'historique ?**

R : Non. L'historique est en **lecture seule** via l'API. C'est une piste d'audit intègre — on ne modifie pas l'historique. Si vous devez nettoyer des données de test, faites-le directement en base (mais pas en production).

**Q : Que se passe-t-il si l'écriture de l'historique échoue ?**

R : L'action métier est tout de même exécutée. L'échec est juste loggé en `WARN`. En d'autres termes : **l'historisation ne bloque jamais** l'application.

**Q : Comment voir l'historique dans Swagger ?**

R : Ouvrez `http://localhost:8080/swagger-ui.html` et cherchez la section **Historique**. Vous pouvez tester les endpoints directement depuis l'interface.

**Q : Une action n'apparaît pas dans l'historique, comment l'ajouter ?**

R : Contactez un développeur pour ajouter l'annotation `@Historise` sur la méthode concernée. Consultez la section 3 de ce guide pour les instructions.
