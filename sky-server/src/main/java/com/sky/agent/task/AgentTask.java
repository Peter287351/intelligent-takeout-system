package com.sky.agent.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 异步任务状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTask {

    public enum Status {
        /** 等待执行 */
        PENDING,
        /** 执行中 */
        RUNNING,
        /** 已完成 */
        COMPLETED,
        /** 失败 */
        FAILED
    }

    /** 任务 ID */
    private String taskId;

    /** 任务状态 */
    private Status status;

    /** 用户输入 */
    private String userInput;

    /** 执行结果（COMPLETED 时有值） */
    private String result;

    /** 错误信息（FAILED 时有值） */
    private String error;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 耗时（毫秒） */
    private Long elapsedMs;
}
