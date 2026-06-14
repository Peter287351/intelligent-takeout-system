package com.sky.agent.controller;

import com.sky.agent.service.MealPlanAgentService;
import com.sky.agent.task.AgentTask;
import com.sky.agent.task.AgentTaskService;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI Agent 接口
 * 提供智能套餐推荐等 Agent 功能
 */
@Slf4j
@RestController
@RequestMapping("/user/agent")
@Api(tags = "AI Agent 智能助手")
public class AgentController {

    @Autowired
    private MealPlanAgentService mealPlanAgentService;

    @Autowired
    private AgentTaskService agentTaskService;

    /**
     * 智能套餐定制（同步，兼容旧接口）
     */
    @PostMapping("/meal-plan")
    @ApiOperation("智能套餐定制（同步）")
    public Result<Map<String, Object>> planMeal(@RequestBody Map<String, String> request) {
        String userInput = request.get("input");
        if (userInput == null || userInput.isBlank()) {
            return Result.error("请输入用餐需求描述");
        }

        log.info("Agent 套餐定制请求(同步): {}", userInput);
        String plan = mealPlanAgentService.planMeal(userInput);

        return Result.success(Map.of(
                "input", userInput,
                "plan", plan
        ));
    }

    /**
     * 智能套餐定制（异步提交）
     * 提交后立即返回 taskId，前端轮询 /meal-plan/result/{taskId} 获取结果
     */
    @PostMapping("/meal-plan/async")
    @ApiOperation("智能套餐定制（异步提交）")
    public Result<Map<String, Object>> planMealAsync(@RequestBody Map<String, String> request) {
        String userInput = request.get("input");
        if (userInput == null || userInput.isBlank()) {
            return Result.error("请输入用餐需求描述");
        }

        log.info("Agent 套餐定制请求(异步): {}", userInput);
        AgentTask task = agentTaskService.submit(userInput);

        return Result.success(Map.of(
                "taskId", task.getTaskId(),
                "status", task.getStatus().name(),
                "message", "任务已提交，请轮询查询结果"
        ));
    }

    /**
     * 查询异步任务结果
     */
    @GetMapping("/meal-plan/result/{taskId}")
    @ApiOperation("查询套餐定制任务结果")
    public Result<Map<String, Object>> getMealPlanResult(@PathVariable String taskId) {
        AgentTask task = agentTaskService.query(taskId);
        if (task == null) {
            return Result.error("任务不存在或已过期");
        }

        return Result.success(Map.of(
                "taskId", task.getTaskId(),
                "status", task.getStatus().name(),
                "plan", task.getResult() != null ? task.getResult() : "",
                "error", task.getError() != null ? task.getError() : "",
                "elapsedMs", task.getElapsedMs() != null ? task.getElapsedMs() : 0
        ));
    }
}
