package com.sky.agent.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能套餐定制 Agent 的状态对象
 * 在 StateGraph 各节点之间流转，每个节点可读/写此状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanState {

    /** 用户原始输入 */
    private String userInput;

    /** 用餐人数 */
    private Integer dinerCount;

    /** 预算上限（元） */
    private BigDecimal budget;

    /** 忌口/饮食限制（如：不吃辣、不吃香菜） */
    @Builder.Default
    private List<String> restrictions = new ArrayList<>();

    /** 菜品要求（如：要有荤菜、要有汤） */
    @Builder.Default
    private List<String> requirements = new ArrayList<>();

    /** 候选菜品/套餐列表（searchDishes 节点产出） */
    @Builder.Default
    private List<MealCandidate> candidates = new ArrayList<>();

    /** 约束满足度评分 0-100（evaluateConstraints 节点产出） */
    private Integer satisfactionScore;

    /** 不满足的问题列表（evaluateConstraints 节点产出） */
    @Builder.Default
    private List<String> issues = new ArrayList<>();

    /** 当前循环次数 */
    @Builder.Default
    private Integer iteration = 0;

    /** 最大循环次数 */
    @Builder.Default
    private Integer maxIterations = 3;

    /** 最终的推荐方案（generatePlan 节点产出） */
    private String finalPlan;

    /** 松弛后的菜品要求（evaluateAndGenerate 节点产出，供 relaxConstraints 使用） */
    @Builder.Default
    private List<String> relaxedRequirements = new ArrayList<>();

    /** 检查点快照，记录每个节点的执行前后状态，用于调试和回滚 */
    @Builder.Default
    private List<String> checkpoints = new ArrayList<>();

    public void addCheckpoint(String nodeName, String phase) {
        checkpoints.add(String.format("[%s] %s - iteration=%d, score=%s, candidates=%d",
                phase, nodeName, iteration, satisfactionScore,
                candidates != null ? candidates.size() : 0));
    }
}
