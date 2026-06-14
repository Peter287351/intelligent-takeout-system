package com.sky.agent.task;

import com.sky.agent.graph.MealPlanGraph;
import com.sky.agent.state.MealPlanState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 异步任务服务
 * 提交任务后立即返回 taskId，通过轮询获取结果
 */
@Slf4j
@Service
public class AgentTaskService {

    @Autowired
    private MealPlanGraph mealPlanGraph;

    /** 内存存储，后续可替换为 Redis */
    private final Map<String, AgentTask> tasks = new ConcurrentHashMap<>();

    /**
     * 提交异步任务，立即返回 taskId
     */
    public AgentTask submit(String userInput) {
        String taskId = UUID.randomUUID().toString().substring(0, 8);

        AgentTask task = AgentTask.builder()
                .taskId(taskId)
                .status(AgentTask.Status.PENDING)
                .userInput(userInput)
                .createTime(LocalDateTime.now())
                .build();

        tasks.put(taskId, task);
        log.info("[AgentTask] 任务已创建: {}，输入: {}", taskId, userInput);

        // 异步执行
        executeAsync(taskId);

        return task;
    }

    /**
     * 异步执行 Agent 流程
     */
    @Async
    public void executeAsync(String taskId) {
        AgentTask task = tasks.get(taskId);
        if (task == null) return;

        task.setStatus(AgentTask.Status.RUNNING);
        log.info("[AgentTask] 任务开始执行: {}", taskId);

        long start = System.currentTimeMillis();
        try {
            MealPlanState result = mealPlanGraph.execute(task.getUserInput());
            String plan = result.getFinalPlan();

            task.setStatus(AgentTask.Status.COMPLETED);
            task.setResult(plan);
            task.setElapsedMs(System.currentTimeMillis() - start);
            task.setFinishTime(LocalDateTime.now());

            log.info("[AgentTask] 任务完成: {}，耗时 {}ms", taskId, task.getElapsedMs());
        } catch (Exception e) {
            log.error("[AgentTask] 任务失败: {}", taskId, e);
            task.setStatus(AgentTask.Status.FAILED);
            task.setError(e.getMessage());
            task.setElapsedMs(System.currentTimeMillis() - start);
            task.setFinishTime(LocalDateTime.now());
        }
    }

    /**
     * 查询任务状态
     */
    public AgentTask query(String taskId) {
        return tasks.get(taskId);
    }

    /**
     * 清理过期任务（超过 30 分钟）
     */
    public void cleanExpired() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        tasks.entrySet().removeIf(entry -> {
            AgentTask task = entry.getValue();
            return task.getCreateTime() != null && task.getCreateTime().isBefore(threshold);
        });
    }
}
