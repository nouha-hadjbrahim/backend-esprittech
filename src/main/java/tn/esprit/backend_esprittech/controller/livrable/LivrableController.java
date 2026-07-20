package tn.esprit.backend_esprittech.controller.livrable;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend_esprittech.dto.livrable.LivrableLinkRequest;
import tn.esprit.backend_esprittech.dto.livrable.LivrableResponse;
import tn.esprit.backend_esprittech.dto.livrable.LivrableUpdateRequest;
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;
import tn.esprit.backend_esprittech.service.livrable.LivrableService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LivrableController {

    private final LivrableService livrableService;

    @PostMapping(value = "/api/projets/{projetId}/livrables/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT')")
    public LivrableResponse upload(
            @PathVariable Long projetId,
            @RequestParam TypeLivrable typeLivrable,
            @RequestParam String nom,
            @RequestParam(required = false) String description,
            @RequestPart("file") MultipartFile file
    ) {
        return livrableService.upload(projetId, typeLivrable, nom, description, file);
    }

    @PostMapping("/api/projets/{projetId}/livrables/link")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT')")
    public LivrableResponse addLink(
            @PathVariable Long projetId,
            @Valid @RequestBody LivrableLinkRequest request
    ) {
        return livrableService.addLink(projetId, request);
    }

    @GetMapping("/api/projets/{projetId}/livrables")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CI')")
    public List<LivrableResponse> findByProjet(@PathVariable Long projetId) {
        return livrableService.findByProjet(projetId);
    }

    @GetMapping("/api/livrables/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CI')")
    public LivrableResponse findById(@PathVariable Long id) {
        return livrableService.findById(id);
    }

    @GetMapping("/api/livrables/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CI')")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
        LivrableService.LivrableDownload download = livrableService.download(id);
        MediaType mediaType = download.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(download.contentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(download.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.filename() + "\"")
                .body(new InputStreamResource(download.inputStream()));
    }

    @PutMapping("/api/livrables/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT')")
    public LivrableResponse update(
            @PathVariable Long id,
            @Valid @RequestBody LivrableUpdateRequest request
    ) {
        return livrableService.update(id, request);
    }

    @DeleteMapping("/api/livrables/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        livrableService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/livrables")
    @PreAuthorize("hasRole('ADMIN')")
    public List<LivrableResponse> findAllAdmin() {
        return livrableService.findAllActiveForAdmin();
    }

    @PutMapping("/api/admin/livrables/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public LivrableResponse updateAdmin(
            @PathVariable Long id,
            @Valid @RequestBody LivrableUpdateRequest request
    ) {
        return livrableService.update(id, request);
    }

    @DeleteMapping("/api/admin/livrables/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        // Meme traitement que la route enseignant, restreinte ici aux administrateurs.
        return delete(id);
    }
}
