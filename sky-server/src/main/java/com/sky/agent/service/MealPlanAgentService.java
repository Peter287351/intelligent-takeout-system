package com.sky.agent.service;

import com.sky.agent.graph.MealPlanGraph;
import com.sky.agent.state.MealPlanState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 智能套餐定制 Agent 服务层
 * 封装 StateGraph 的执行入口，对外提供简洁的 API
 */
@Slf4j
@Service
public class MealPlanAgentService {

    @Autowired
    private MealPlanGraph mealPlanGraph;

    /**
     * 根据用户需求推荐套餐
     * @param userInput 用户自然语言输入（如："3人晚餐，预算100，不吃辣，要有荤素汤"）
     * @return Agent 生成的推荐方案
     */
    public String planMeal(String userInput) {
        log.info("收到套餐定制请求: {}", userInput);
        long start = System.currentTimeMillis();

        MealPlanState result = mealPlanGraph.execute(userInput);

        long elapsed = System.currentTimeMillis() - start;
        log.info("套餐定制完成，耗时 {}ms，循环 {} 次，checkpoints: {}",
                elapsed, result.getIteration(), result.getCheckpoints());

        return result.getFinalPlan();
    }
}
