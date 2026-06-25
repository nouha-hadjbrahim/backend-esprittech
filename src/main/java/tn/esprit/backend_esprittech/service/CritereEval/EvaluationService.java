package tn.esprit.backend_esprittech.service.CritereEval;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.CritereEval.*;
import tn.esprit.backend_esprittech.entity.CritereEval.*;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.exception.CritereEval.BadRequestException;
import tn.esprit.backend_esprittech.exception.CritereEval.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereEliminatoireRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.CritereNoteRepository;
import tn.esprit.backend_esprittech.repository.CritereEval.EvaluationRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class EvaluationService {

    private final SujetProjetRepository sujetProjetRepository;
    private final CritereEliminatoireRepository critereEliminatoireRepository;
    private final CritereNoteRepository critereNoteRepository;
    private final EvaluationRepository evaluationRepository;

    public EvaluationResponse calculerEvaluation(Long projetId, EvaluationRequest request) {
        SujetProjet projet = sujetProjetRepository.findById(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec id: " + projetId));

        if (projet.getStatut() != StatutSujet.REALISATION_TERMINEE) {
            throw new BadRequestException("Le projet doit être en statut REALISATION_TERMINEE pour être évalué.");
        }

        List<CritereEliminatoire> criteresEliminatoires =
                critereEliminatoireRepository.findByActifTrueOrderByOrdreAsc();

        List<CritereNote> criteresNotes =
                critereNoteRepository.findByActifTrueOrderByOrdreAsc();

        Map<Long, ResultatEliminatoireRequest> resultatsEliminatoires =
                mapEliminatoires(request.eliminatoires());

        Map<Long, ResultatNoteRequest> resultatsNotes =
                mapNotes(request.notes());

        Evaluation evaluation = new Evaluation();
        evaluation.setSujetProjet(projet);

        boolean bloqueParEliminatoire = false;

        for (CritereEliminatoire critere : criteresEliminatoires) {
            ResultatEliminatoireRequest resultatRequest = resultatsEliminatoires.get(critere.getId());

            if (resultatRequest == null) {
                throw new BadRequestException("Résultat manquant pour le critère éliminatoire id: " + critere.getId());
            }

            ResultatCritere resultat = new ResultatCritere();
            resultat.setCritereId(critere.getId());
            resultat.setTypeCritere(TypeCritere.ELIMINATOIRE);
            resultat.setReponseEliminatoire(resultatRequest.reponse());
            resultat.setCommentaire(resultatRequest.commentaire());

            evaluation.addResultat(resultat);

            if (resultatRequest.reponse() == ReponseEliminatoire.NOT_OK) {
                bloqueParEliminatoire = true;
            }
        }

        double sommeScoresPonderes = 0.0;
        double sommePoids = 0.0;

        for (CritereNote critere : criteresNotes) {
            ResultatNoteRequest resultatRequest = resultatsNotes.get(critere.getId());

            if (resultatRequest == null) {
                throw new BadRequestException("Résultat manquant pour le critère noté id: " + critere.getId());
            }

            validateNoteObtenue(resultatRequest.noteObtenue(), critere);

            double scorePondere = (resultatRequest.noteObtenue() / critere.getBareme()) * critere.getPoids();

            sommeScoresPonderes += scorePondere;
            sommePoids += critere.getPoids();

            ResultatCritere resultat = new ResultatCritere();
            resultat.setCritereId(critere.getId());
            resultat.setTypeCritere(TypeCritere.NOTE);
            resultat.setNoteObtenue(resultatRequest.noteObtenue());
            resultat.setScorePondere(scorePondere);
            resultat.setCommentaire(resultatRequest.commentaire());

            evaluation.addResultat(resultat);
        }

        double scoreFinal = 0.0;

        if (sommePoids > 0) {
            scoreFinal = (sommeScoresPonderes / sommePoids) * 100;
        }

        boolean eligibleIndustrialisation = !bloqueParEliminatoire;

        evaluation.setScoreFinal(scoreFinal);
        evaluation.setBloqueParEliminatoire(bloqueParEliminatoire);
        evaluation.setEligibleIndustrialisation(eligibleIndustrialisation);

        if (bloqueParEliminatoire) {
            evaluation.setCommentaire("Projet non éligible : au moins un critère éliminatoire est NOT_OK.");
        } else {
            evaluation.setCommentaire("Projet évalué avec succès.");
        }

        Evaluation savedEvaluation = evaluationRepository.save(evaluation);

        projet.setScoreFinalEvaluation(scoreFinal);
        projet.setEligibleIndustrialisation(eligibleIndustrialisation);
        projet.setBloqueParEliminatoire(bloqueParEliminatoire);

        sujetProjetRepository.save(projet);

        return toResponse(savedEvaluation);
    }

    @Transactional(readOnly = true)
    public EvaluationResponse getLatestEvaluationByProjet(Long projetId) {
        Evaluation evaluation = evaluationRepository.findTopBySujetProjetIdOrderByDateCalculDesc(projetId)
                .orElseThrow(() -> new ResourceNotFoundException("Aucune évaluation trouvée pour le projet id: " + projetId));

        return toResponse(evaluation);
    }

    private Map<Long, ResultatEliminatoireRequest> mapEliminatoires(List<ResultatEliminatoireRequest> requests) {
        Map<Long, ResultatEliminatoireRequest> map = new HashMap<>();

        if (requests == null) {
            return map;
        }

        for (ResultatEliminatoireRequest request : requests) {
            map.put(request.critereId(), request);
        }

        return map;
    }

    private Map<Long, ResultatNoteRequest> mapNotes(List<ResultatNoteRequest> requests) {
        Map<Long, ResultatNoteRequest> map = new HashMap<>();

        if (requests == null) {
            return map;
        }

        for (ResultatNoteRequest request : requests) {
            map.put(request.critereId(), request);
        }

        return map;
    }

    private void validateNoteObtenue(Double noteObtenue, CritereNote critere) {
        if (noteObtenue == null) {
            throw new BadRequestException("La note obtenue est obligatoire pour le critère id: " + critere.getId());
        }

        if (critere.getBareme() == null || critere.getBareme() <= 0) {
            throw new BadRequestException("Le barème du critère doit être supérieur à 0.");
        }

        if (critere.getPoids() == null || critere.getPoids() <= 0) {
            throw new BadRequestException("Le poids du critère doit être supérieur à 0.");
        }

        if (noteObtenue < 0) {
            throw new BadRequestException("La note obtenue ne peut pas être négative.");
        }

        if (noteObtenue > critere.getBareme()) {
            throw new BadRequestException(
                    "La note obtenue ne peut pas dépasser le barème du critère id: " + critere.getId()
            );
        }
    }

    private EvaluationResponse toResponse(Evaluation evaluation) {
        List<ResultatCritereResponse> resultats = evaluation.getResultats()
                .stream()
                .map(resultat -> new ResultatCritereResponse(
                        resultat.getId(),
                        resultat.getCritereId(),
                        resultat.getTypeCritere(),
                        resultat.getReponseEliminatoire(),
                        resultat.getNoteObtenue(),
                        resultat.getScorePondere(),
                        resultat.getCommentaire()
                ))
                .toList();

        return new EvaluationResponse(
                evaluation.getId(),
                evaluation.getSujetProjet().getId(),
                evaluation.getScoreFinal(),
                evaluation.getEligibleIndustrialisation(),
                evaluation.getBloqueParEliminatoire(),
                evaluation.getDateCalcul(),
                evaluation.getCommentaire(),
                resultats
        );
    }
}