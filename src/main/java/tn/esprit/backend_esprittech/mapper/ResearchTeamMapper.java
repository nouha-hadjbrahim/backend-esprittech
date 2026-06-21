package tn.esprit.backend_esprittech.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.backend_esprittech.dto.request.CreateResearchTeamRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateResearchTeamRequest;
import tn.esprit.backend_esprittech.dto.response.ResearchTeamResponse;
import tn.esprit.backend_esprittech.dto.response.ResearchTeamUserResponse;
import tn.esprit.backend_esprittech.entity.ResearchTeam;
import tn.esprit.backend_esprittech.entity.User;

@Component
public class ResearchTeamMapper {

	public ResearchTeam toEntity(CreateResearchTeamRequest request) {
		if (request == null) {
			return null;
		}
		return ResearchTeam.builder()
				.nom(request.nom())
				.domaineRecherche(request.domaineRecherche())
				.description(request.description())
				.build();
	}

	public void updateEntity(UpdateResearchTeamRequest request, ResearchTeam researchTeam) {
		if (request == null || researchTeam == null) {
			return;
		}
		if (request.nom() != null) {
			researchTeam.setNom(request.nom());
		}
		if (request.domaineRecherche() != null) {
			researchTeam.setDomaineRecherche(request.domaineRecherche());
		}
		if (request.description() != null) {
			researchTeam.setDescription(request.description());
		}
	}

	public ResearchTeamResponse toResponse(ResearchTeam researchTeam) {
		if (researchTeam == null) {
			return null;
		}
		return new ResearchTeamResponse(
				researchTeam.getId(),
				researchTeam.getNom(),
				researchTeam.getDomaineRecherche(),
				researchTeam.getDescription(),
				toUserResponse(researchTeam.getLeader()),
				researchTeam.getMembers().stream().map(this::toUserResponse).toList()
		);
	}

	public ResearchTeamUserResponse toUserResponse(User user) {
		if (user == null) {
			return null;
		}
		return new ResearchTeamUserResponse(user.getId(), user.getNom() + " " + user.getPrenom());
	}
}