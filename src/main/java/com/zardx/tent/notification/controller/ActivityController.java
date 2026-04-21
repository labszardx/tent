package com.zardx.tent.notification.controller;

import com.zardx.tent.notification.persistence.mongo.Activity;
import com.zardx.tent.notification.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /**
     * Fetches paginated activities for a list of group IDs.
     * This powers the global activity feed and the Activity tab in the bottom nav.
     * * @param groupIds List of group IDs the user belongs to
     * @param page Zero-based page index (default 0)
     * @param size Number of items per page (default 10)
     */
    @PostMapping("/fetch")
    public ResponseEntity<Page<Activity>> getActivities(
            @RequestBody List<String> groupIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Validation to ensure the list isn't empty
        if (groupIds == null || groupIds.isEmpty()) {
            return ResponseEntity.ok(Page.empty());
        }

        Page<Activity> activities = activityService.getActivitiesByGroupIds(groupIds, page, size);
        return ResponseEntity.ok(activities);
    }

    /**
     * Fetches activities for a specific group.
     * Useful for the "Activity History" section inside the Group Details view.
     * * @param groupId The specific group ID
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<Page<Activity>> getGroupActivities(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<Activity> activities = activityService.getActivitiesByGroupIds(List.of(groupId), page, size);
        return ResponseEntity.ok(activities);
    }
}