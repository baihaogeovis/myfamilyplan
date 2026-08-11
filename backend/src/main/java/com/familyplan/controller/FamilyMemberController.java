package com.familyplan.controller;

import com.familyplan.common.Result;
import com.familyplan.entity.FamilyMember;
import com.familyplan.security.SecurityUtil;
import com.familyplan.service.IFamilyMemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 家庭成员控制器(按当前登录家庭管理成员)
 */
@Slf4j
@RestController
@RequestMapping("/member")
public class FamilyMemberController {

    @Autowired
    private IFamilyMemberService memberService;

    /**
     * 获取当前家庭成员列表
     */
    @GetMapping("/list")
    public Result<List<FamilyMember>> list() {
        Long familyId = SecurityUtil.getRequiredFamilyId();
        List<FamilyMember> list = memberService.getListByFamilyId(familyId);
        return Result.success(list);
    }

    /**
     * 添加成员(自动归属当前家庭)
     */
    @PostMapping
    public Result<Void> add(@RequestBody FamilyMember member) {
        Long familyId = SecurityUtil.getRequiredFamilyId();
        member.setId(null);
        member.setFamilyId(familyId);
        memberService.addMember(member);
        return Result.success();
    }

    /**
     * 更新成员(仅限当前家庭的成员)
     */
    @PutMapping
    public Result<Void> update(@RequestBody FamilyMember member) {
        Long familyId = SecurityUtil.getRequiredFamilyId();
        memberService.checkMemberBelongsToFamily(familyId, member.getId());
        member.setFamilyId(familyId);
        memberService.updateMember(member);
        return Result.success();
    }

    /**
     * 删除成员(仅限当前家庭的成员)
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long familyId = SecurityUtil.getRequiredFamilyId();
        memberService.checkMemberBelongsToFamily(familyId, id);
        memberService.deleteMember(id);
        return Result.success();
    }
}
