package com.familyplan.controller;

import com.familyplan.common.Result;
import com.familyplan.entity.FamilyMember;
import com.familyplan.service.IFamilyMemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 家庭成员控制器
 */
@Slf4j
@RestController
@RequestMapping("/member")
public class FamilyMemberController {
    
    @Autowired
    private IFamilyMemberService memberService;
    
    /**
     * 获取成员列表
     */
    @GetMapping("/list")
    public Result<List<FamilyMember>> list(@RequestParam Long userId) {
        List<FamilyMember> list = memberService.getListByUserId(userId);
        return Result.success(list);
    }
    
    /**
     * 添加成员
     */
    @PostMapping
    public Result<Void> add(@RequestBody FamilyMember member) {
        memberService.addMember(member);
        return Result.success();
    }
    
    /**
     * 更新成员
     */
    @PutMapping
    public Result<Void> update(@RequestBody FamilyMember member) {
        memberService.updateMember(member);
        return Result.success();
    }
    
    /**
     * 删除成员
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        memberService.deleteMember(id);
        return Result.success();
    }
}
