package com.tinybrain.agent.controller;

import com.tinybrain.agent.dto.SkillConfig;
import com.tinybrain.agent.dto.SkillInfo;
import com.tinybrain.agent.dto.SkillDistillRequest;
import com.tinybrain.agent.service.SkillService;
import com.tinybrain.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Skill 管理控制器
 * <p>
 * 提供 Skill 的创建、编辑、删除、蒸馏等功能。
 */
@Tag(name = "06-Skill 管理", description = "Skill 创建、编辑、删除、蒸馏")
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @Operation(summary = "获取所有 Skill", description = "获取已安装的所有 Skill 列表")
    @GetMapping("/list")
    public R<List<SkillInfo>> listSkills() {
        return R.ok(skillService.listSkills());
    }

    @Operation(summary = "获取 Skill 详情", description = "获取指定 Skill 的详细信息")
    @GetMapping("/{id}")
    public R<SkillInfo> getSkill(@PathVariable String id) {
        return R.ok(skillService.getSkill(id));
    }

    @Operation(summary = "创建 Skill", description = "手动创建一个新的 Skill")
    @PostMapping("/create")
    public R<SkillInfo> createSkill(@Valid @RequestBody SkillConfig config) {
        return R.ok(skillService.createSkill(config));
    }

    @Operation(summary = "更新 Skill", description = "更新指定 Skill 的配置")
    @PutMapping("/{id}")
    public R<SkillInfo> updateSkill(@PathVariable String id, @Valid @RequestBody SkillConfig config) {
        return R.ok(skillService.updateSkill(id, config));
    }

    @Operation(summary = "删除 Skill", description = "删除指定的 Skill")
    @DeleteMapping("/{id}")
    public R<Void> deleteSkill(@PathVariable String id) {
        skillService.deleteSkill(id);
        return R.okMsg("Skill 已删除");
    }

    @Operation(summary = "启用/禁用 Skill", description = "切换 Skill 的启用状态")
    @PostMapping("/{id}/toggle")
    public R<SkillInfo> toggleSkill(@PathVariable String id) {
        return R.ok(skillService.toggleSkill(id));
    }

    @Operation(summary = "蒸馏 Skill", description = "从对话历史或文档中蒸馏出一个新的 Skill")
    @PostMapping("/distill")
    public R<SkillInfo> distillSkill(@Valid @RequestBody SkillDistillRequest request) {
        return R.ok(skillService.distillSkill(request));
    }

    @Operation(summary = "安装 Skill", description = "从 Skill 市场安装一个 Skill")
    @PostMapping("/install")
    public R<SkillInfo> installSkill(@RequestBody Map<String, String> request) {
        String skillId = request.get("skillId");
        return R.ok(skillService.installSkill(skillId));
    }

    @Operation(summary = "获取 Skill 市场列表", description = "获取可用的 Skill 市场列表")
    @GetMapping("/market")
    public R<List<SkillInfo>> getMarketSkills() {
        return R.ok(skillService.getMarketSkills());
    }
}
