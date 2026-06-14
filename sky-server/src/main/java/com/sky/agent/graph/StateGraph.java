package com.sky.agent.graph;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 通用 StateGraph 框架
 * 实现了 langgraph4j 的核心概念：Node、Edge、ConditionalEdge、Checkpoint
 *
 * @param <S> 状态类型，在各节点之间流转
 */
@Slf4j
public class StateGraph<S> {

    /** 节点：名称 → 处理函数 */
    private final Map<String, Function<S, S>> nodes = new LinkedHashMap<>();

    /** 固定边：fromNode → toNode（无条件跳转） */
    private final Map<String, String> edges = new LinkedHashMap<>();

    /** 条件边：fromNode → 路由器函数 + 路由表 */
    private final Map<String, ConditionalEdge<S>> conditionalEdges = new LinkedHashMap<>();

    private String entryPoint;
    private String finishPoint;

    // ==================== Builder 风格 API ====================

    public StateGraph<S> addNode(String name, Function<S, S> function) {
        nodes.put(name, function);
        return this;
    }

    /** 添加固定边：from 执行完后必定跳转到 to */
    public StateGraph<S> addEdge(String from, String to) {
        edges.put(from, to);
        return this;
    }

    /**
     * 添加条件边：from 执行完后，调用 router 函数根据状态决定跳转目标
     * @param router   路由决策函数，根据当前状态返回路由键
     * @param routeMap 路由键 → 下个节点名 的映射
     */
    public StateGraph<S> addConditionalEdge(String from,
                                             Function<S, String> router,
                                             Map<String, String> routeMap) {
        conditionalEdges.put(from, new ConditionalEdge<>(router, routeMap));
        return this;
    }

    public StateGraph<S> setEntryPoint(String name) {
        this.entryPoint = name;
        return this;
    }

    public StateGraph<S> setFinishPoint(String name) {
        this.finishPoint = name;
        return this;
    }

    // ==================== 图执行引擎 ====================

    /**
     * 执行状态图
     * @param state 初始状态
     * @return 经过所有节点处理后的最终状态
     */
    public S invoke(S state) {
        if (entryPoint == null || finishPoint == null) {
            throw new IllegalStateException("必须设置 entryPoint 和 finishPoint");
        }

        String currentNode = entryPoint;
        int totalSteps = 0;
        int maxSteps = 50; // 防止死循环

        log.info("StateGraph 开始执行，入口节点: {}", entryPoint);

        while (!currentNode.equals(finishPoint)) {
            if (totalSteps++ >= maxSteps) {
                log.error("StateGraph 执行步数超过上限 {}，强制终止", maxSteps);
                break;
            }

            Function<S, S> nodeFunc = nodes.get(currentNode);
            if (nodeFunc == null) {
                log.error("未找到节点: {}", currentNode);
                break;
            }

            log.info("[Step {}] 执行节点: {}", totalSteps, currentNode);

            // 执行前快照（Checkpoint）
            recordCheckpoint(state, currentNode, "before");

            // 执行节点
            state = nodeFunc.apply(state);

            // 执行后快照
            recordCheckpoint(state, currentNode, "after");

            // 决定下一个节点
            currentNode = determineNextNode(currentNode, state);
            log.info("[Step {}] 跳转到: {}", totalSteps, currentNode);
        }

        // 执行终止节点
        if (nodes.containsKey(finishPoint)) {
            log.info("执行终止节点: {}", finishPoint);
            state = nodes.get(finishPoint).apply(state);
        }

        log.info("StateGraph 执行完成，共 {} 步", totalSteps);
        return state;
    }

    private String determineNextNode(String currentNode, S state) {
        // 条件边优先
        if (conditionalEdges.containsKey(currentNode)) {
            ConditionalEdge<S> ce = conditionalEdges.get(currentNode);
            String routeKey = ce.router.apply(state);
            String nextNode = ce.routeMap.getOrDefault(routeKey, finishPoint);
            log.info("条件路由: {} → key={} → {}", currentNode, routeKey, nextNode);
            return nextNode;
        }
        // 其次固定边
        if (edges.containsKey(currentNode)) {
            return edges.get(currentNode);
        }
        // 默认结束
        return finishPoint;
    }

    /** 记录 checkpoint，供调试和回滚使用 */
    @SuppressWarnings("unchecked")
    private void recordCheckpoint(S state, String nodeName, String phase) {
        try {
            // 通过反射调用 state 的 addCheckpoint 方法（如果存在）
            Method method = state.getClass().getMethod("addCheckpoint", String.class, String.class);
            method.invoke(state, nodeName, phase);
        } catch (Exception e) {
            // state 没有 addCheckpoint 方法，忽略
        }
    }

    // ==================== 内部类 ====================

    @AllArgsConstructor
    private static class ConditionalEdge<S> {
        /** 路由决策函数 */
        Function<S, String> router;
        /** 路由键 → 目标节点 */
        Map<String, String> routeMap;
    }

    // ==================== 工厂方法 ====================

    public static <S> StateGraph<S> create() {
        return new StateGraph<>();
    }
}
