package tn.esprit.backend_esprittech.dto.dashboard.extended;

public record RecentActivityItem(
        long id,
        String message,
        String actor,
        String time,
        String type
) {}
