package tn.esprit.backend_esprittech.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend_esprittech.entity.ReferentielUtilisateur;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.exception.ReferentielNotFoundException;
import tn.esprit.backend_esprittech.repository.ReferentielUtilisateurRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Gestion du référentiel : import du CSV (source de vérité) et vérification
 * de correspondance lors de l'inscription d'un utilisateur.
 */
@Service
public class ReferentielService {

    private static final Logger log = LoggerFactory.getLogger(ReferentielService.class);
    private static final int NB_COLONNES = 6;

    private final ReferentielUtilisateurRepository referentielRepository;

    public ReferentielService(ReferentielUtilisateurRepository referentielRepository) {
        this.referentielRepository = referentielRepository;
    }

    /**
     * Parse un fichier CSV (format : nom,prenom,email,identifiant,type,telephone)
     * et sauvegarde chaque ligne valide dans le référentiel.
     * La ligne d'en-tête est ignorée ; les lignes invalides sont loguées et ignorées.
     *
     * @return le nombre de lignes importées avec succès
     */
    @Transactional
    public int importerCsv(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Le fichier CSV est vide ou absent");
        }

        int importes = 0;
        int numeroLigne = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(fichier.getInputStream(), StandardCharsets.UTF_8))) {

            String ligne;
            while ((ligne = reader.readLine()) != null) {
                numeroLigne++;

                // Ignore la ligne d'en-tête et les lignes vides
                if (numeroLigne == 1 || !StringUtils.hasText(ligne)) {
                    continue;
                }

                ReferentielUtilisateur entree = parserLigne(ligne, numeroLigne);
                if (entree != null) {
                    referentielRepository.save(entree);
                    importes++;
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Impossible de lire le fichier CSV : " + e.getMessage());
        }

        log.info("Import référentiel terminé : {} ligne(s) importée(s).", importes);
        return importes;
    }

    /**
     * Vérifie qu'une correspondance existe dans le référentiel par email.
     *
     * @throws ReferentielNotFoundException si aucune correspondance n'est trouvée
     */
    public ReferentielUtilisateur trouverCorrespondance(String email) {
        return referentielRepository.findByEmail(email)
                .orElseThrow(() -> new ReferentielNotFoundException(
                        "Utilisateur non trouvé dans le référentiel. Vérifiez votre email."));
    }

    /** Transforme une ligne CSV en entité, ou renvoie null si la ligne est invalide. */
    private ReferentielUtilisateur parserLigne(String ligne, int numeroLigne) {
        String[] champs = ligne.split(",", -1);
        if (champs.length < NB_COLONNES) {
            log.warn("Ligne {} ignorée (colonnes insuffisantes) : {}", numeroLigne, ligne);
            return null;
        }

        String nom = champs[0].trim();
        String prenom = champs[1].trim();
        String email = champs[2].trim();
        String identifiant = champs[3].trim();
        String typeBrut = champs[4].trim().toUpperCase();
        String telephone = champs[5].trim();

        if (!StringUtils.hasText(email) || !StringUtils.hasText(identifiant)) {
            log.warn("Ligne {} ignorée (email ou identifiant vide) : {}", numeroLigne, ligne);
            return null;
        }

        TypeUtilisateur type;
        try {
            type = TypeUtilisateur.valueOf(typeBrut);
        } catch (IllegalArgumentException e) {
            log.warn("Ligne {} ignorée (type invalide '{}') : {}", numeroLigne, typeBrut, ligne);
            return null;
        }

        // Mise à jour si une entrée existe déjà pour cet email, sinon création
        ReferentielUtilisateur entree = referentielRepository.findByEmail(email)
                .orElseGet(ReferentielUtilisateur::new);
        entree.setNom(nom);
        entree.setPrenom(prenom);
        entree.setEmail(email);
        entree.setIdentifiant(identifiant);
        entree.setType(type);
        entree.setTelephone(telephone);
        return entree;
    }
}
