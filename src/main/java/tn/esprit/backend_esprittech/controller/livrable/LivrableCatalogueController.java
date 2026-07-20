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
import tn.esprit.backend_esprittech.dto.livrable.LivrableCatalogueResponse;
import tn.esprit.backend_esprittech.dto.livrable.LivrableLinkRequest;
import tn.esprit.backend_esprittech.dto.livrable.LivrableUpdateRequest;
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;
import tn.esprit.backend_esprittech.service.livrable.LivrableCatalogueService;

import java.util.List;

/**
 * Endpoints des livrables rattachés aux projets du catalogue applicatif.
 */
@RestController
@RequiredArgsConstructor
public class LivrableCatalogueController {

    private final LivrableCatalogueService livrableService;

    @PostMapping(value = "/api/projets-catalogue/{projetId}/livrables/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT')")
    public LivrableCatalogueResponse upload(
            @PathVariable Long projetId,
            @RequestParam TypeLivrable typeLivrable,
            @RequestParam String nom,
            @RequestParam(required = false) String description,
            @RequestPart("file") MultipartFile file
    ) {
        return livrableService.upload(projetId, typeLivrable, nom, description, file);
    }

    @PostMapping("/api/projets-catalogue/{projetId}/livrables/link")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT')")
    public LivrableCatalogueResponse addLink(
            @PathVariable Long projetId,
            @Valid @RequestBody LivrableLinkRequest request
    ) {
        return livrableService.addLink(projetId, request);
    }

    @GetMapping("/api/projets-catalogue/{projetId}/livrables")
    @PreAuthorize("isAuthenticated()")
    public List<LivrableCatalogueResponse> findByProjet(@PathVariable Long projetId) {
        return livrableService.findByProjet(projetId);
    }

    @GetMapping("/api/livrables-catalogue/{id}")
    @PreAuthorize("isAuthenticated()")
    public LivrableCatalogueResponse findById(@PathVariable Long id) {
        return livrableService.findById(id);
    }

    @GetMapping("/api/livrables-catalogue/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
        LivrableCatalogueService.LivrableDownload download = livrableService.download(id);
        MediaType mediaType = download.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(download.contentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(download.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.filename() + "\"")
                .body(new InputStreamResource(download.inputStream()));
    }

    @PutMapping("/api/livrables-catalogue/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT')")
    public LivrableCatalogueResponse update(
            @PathVariable Long id,
            @Valid @RequestBody LivrableUpdateRequest request
    ) {
        return livrableService.update(id, request);
    }

    @DeleteMapping("/api/livrables-catalogue/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        livrableService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
