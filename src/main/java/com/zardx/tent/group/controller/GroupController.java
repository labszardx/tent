package com.zardx.tent.group.controller;

import com.zardx.tent.group.model.Group;
import com.zardx.tent.group.model.SettlementPlan;
import com.zardx.tent.group.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
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
            @RequestBody List<Group.GroupMember> members) {

        return ResponseEntity.ok(groupService.addMembers(groupId, requesterId, members));
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

    // GET /api/v1/groups/{userId}
    @GetMapping("")
    public ResponseEntity<List<Group>> getGroupsById(
            @RequestParam String userId) {

        return ResponseEntity.ok(groupService.getGroupsByUserId(userId));
    }

    // DELETE /api/v1/groups/{groupId}/members/{targetUserId}?requesterId=...
    @DeleteMapping("/{groupId}/members/{targetUserId}")
    public ResponseEntity<Group> removeMember(
            @PathVariable String groupId,
            @PathVariable String targetUserId,
            @RequestParam String requesterId) {

        return ResponseEntity.ok(groupService.removeMember(groupId, requesterId, targetUserId));
    }

    @PatchMapping("/{groupId}/members/{userId}/nickname")
    public ResponseEntity<Group> updateNickname(
            @PathVariable String groupId,
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {

        String newNickname = request.get("nickname");
        if (newNickname == null || newNickname.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Group updatedGroup = groupService.updateMemberNickname(groupId, userId, newNickname);
        return ResponseEntity.ok(updatedGroup);
    }
}