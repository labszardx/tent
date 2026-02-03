package com.zardx.tent.group.controller;

import com.zardx.tent.group.model.Group;
import com.zardx.tent.group.model.SettlementPlan;
import com.zardx.tent.group.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<Group> create(@Valid @RequestBody Group group) {
        return ResponseEntity.ok(groupService.createGroup(group));
    }

    // POST /api/v1/groups/{groupId}/members?requesterId=user_alice
    @PostMapping("/{groupId}/members")
    public ResponseEntity<Group> addMember(
            @PathVariable String groupId,
            @RequestParam String requesterId,
            @RequestBody Group.GroupMember member) {

        return ResponseEntity.ok(groupService.addMember(groupId, requesterId, member));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Group> get(@PathVariable String id) {
        return ResponseEntity.ok(groupService.getGroup(id));
    }

    @PatchMapping("/{groupId}/members/{targetUserId}/admin")
    public ResponseEntity<Group> toggleAdmin(
            @PathVariable String groupId,
            @PathVariable String targetUserId,
            @RequestParam String requesterId,
            @RequestParam boolean isAdmin) {
        return ResponseEntity.ok(groupService.toggleAdminRights(groupId, requesterId, targetUserId, isAdmin));
    }

    // GET /api/v1/groups/{groupId}/settlements/{userId}
    @GetMapping("/{groupId}/settlements/{userId}")
    public ResponseEntity<SettlementPlan> getSettlementPlan(
            @PathVariable String groupId,
            @PathVariable String userId) {

        return ResponseEntity.ok(groupService.getSettlementPlan(groupId, userId));
    }

    // DELETE /api/v1/groups/{groupId}/members/{targetUserId}?requesterId=...
    @DeleteMapping("/{groupId}/members/{targetUserId}")
    public ResponseEntity<Group> removeMember(
            @PathVariable String groupId,
            @PathVariable String targetUserId,
            @RequestParam String requesterId) {

        return ResponseEntity.ok(groupService.removeMember(groupId, requesterId, targetUserId));
    }
}