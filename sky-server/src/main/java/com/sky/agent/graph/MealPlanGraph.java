package com.sky.agent.graph;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.agent.client.MiMoClient;
import com.sky.agent.state.MealCandidate;
import com.sky.agent.state.MealPlanState;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 智能套餐定制 Agent 的 StateGraph 定义
 *
 * 图结构（优化后，合并 LLM 调用）：
 *   解析需求 → 搜索候选 → 评估+生成方案
 *                ↑            ↓
 *                └── 松弛约束 ←┘（满足度不足时，纯状态操作，不调用LLM）
 *
 * 4 个节点，2 条固定边，1 条条件边，2 次 LLM 调用（parseRequirements + evaluateAndGenerate）
 */
@Slf4j
@Component
public class MealPlanGraph {

    @Autowired
    private MiMoClient miMoClient;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    private StateGraph<MealPlanState> graph;

    @PostConstruct
    public void init() {
        graph = StateGraph.<MealPlanState>create()
                .addNode("parseRequirements", this::parseRequirements)
                .addNode("searchDishes", this::searchDishes)
                .addNode("evaluateAndGenerate", this::evaluateAndGenerate)
                .addNode("relaxConstraints", this::relaxConstraints)

                // 固定边
                .addEdge("parseRequirements", "searchDishes")
                .addEdge("searchDishes", "evaluateAndGenerate")
                .addEdge("relaxConstraints", "searchDishes")

                // 条件边：评估后决定是生成方案还是松弛约束
                .addConditionalEdge("evaluateAndGenerate",
                        this::routeAfterEvaluate,
                        Map.of("generate", "__FINISH__",
                               "relax", "relaxConstraints"))

                .setEntryPoint("parseRequirements")
                .setFinishPoint("__FINISH__");

        log.info("MealPlanGraph StateGraph 初始化完成");
    }

    /**
     * 执行套餐定制流程
     */
    public MealPlanState execute(String userInput) {
        MealPlanState initialState = MealPlanState.builder()
                .userInput(userInput)
                .iteration(0)
                .maxIterations(3)
                .build();
        return graph.invoke(initialState);
    }

    // ==================== 节点 1: 解析需求 ====================

    private MealPlanState parseRequirements(MealPlanState state) {
        log.info("[parseRequirements] 解析用户输入: {}", state.getUserInput());

        String prompt = """
                你是一个点餐助手。从用户输入中提取以下信息，只返回严格JSON，不要任何其他文字：

                {
                  "dinerCount": 用餐人数（整数，默认1）,
                  "budget": 预算金额元（数字，默认0表示无预算限制）,
                  "restrictions": 饮食限制数组（如["不吃辣","不吃香菜"]）,
                  "requirements": 菜品要求数组（如["要有荤菜","要有汤","要有素菜"]）
                }

                用户输入：%s""".formatted(state.getUserInput());

        String json = callLlm(
                "你是一个专业的点餐需求分析助手。",
                prompt,
                true  // 禁用 thinking，结构化提取不需要深度推理
        );

        try {
            JSONObject obj = JSON.parseObject(json);
            state.setDinerCount(obj.getInteger("dinerCount"));
            state.setBudget(obj.getBigDecimal("budget"));
            state.setRestrictions(parseStringList(obj, "restrictions"));
            state.setRequirements(parseStringList(obj, "requirements"));

            log.info("[parseRequirements] 解析结果: dinerCount={}, budget={}, restrictions={}, requirements={}",
                    state.getDinerCount(), state.getBudget(),
                    state.getRestrictions(), state.getRequirements());
        } catch (Exception e) {
            log.warn("[parseRequirements] JSON解析失败，使用默认值。原始响应: {}", json, e);
            if (state.getDinerCount() == null) state.setDinerCount(1);
            if (state.getBudget() == null) state.setBudget(BigDecimal.ZERO);
            if (state.getRestrictions() == null) state.setRestrictions(new ArrayList<>());
            if (state.getRequirements() == null) state.setRequirements(new ArrayList<>());
        }

        return state;
    }

    // ==================== 节点 2: 搜索候选菜品 ====================

    private MealPlanState searchDishes(MealPlanState state) {
        log.info("[searchDishes] 开始搜索候选菜品，迭代次数: {}", state.getIteration());

        // 针对松弛后的搜索：如果迭代次数 > 0，放宽预算 15%
        BigDecimal effectiveBudget = state.getBudget();
        if (state.getIteration() > 0 && effectiveBudget != null && effectiveBudget.compareTo(BigDecimal.ZERO) > 0) {
            effectiveBudget = effectiveBudget.multiply(new BigDecimal("1.15"));
            log.info("[searchDishes] 松弛预算: {} → {}", state.getBudget(), effectiveBudget);
        }

        List<MealCandidate> candidates = new ArrayList<>();

        // 查询菜品分类下的所有在售菜品
        List<Category> dishCategories = categoryMapper.list(1);
        for (Category cat : dishCategories) {
            Dish query = Dish.builder().categoryId(cat.getId()).status(1).build();
            List<Dish> dishes = dishMapper.list(query);
            for (Dish dish : dishes) {
                if (effectiveBudget != null && effectiveBudget.compareTo(BigDecimal.ZERO) > 0) {
                    int diners = Math.max(state.getDinerCount() != null ? state.getDinerCount() : 1, 1);
                    BigDecimal perPersonBudget = effectiveBudget.divide(BigDecimal.valueOf(diners), 2,
                            java.math.RoundingMode.HALF_UP);
                    // 单道菜不超过人均预算的 60%
                    if (dish.getPrice().compareTo(perPersonBudget.multiply(new BigDecimal("0.6"))) > 0) {
                        continue;
                    }
                }

                List<DishFlavor> flavors = dishFlavorMapper.getByDishId(dish.getId());
                List<String> flavorNames = flavors.stream()
                        .map(DishFlavor::getName)
                        .collect(Collectors.toList());

                candidates.add(MealCandidate.builder()
                        .id(dish.getId())
                        .name(dish.getName())
                        .price(dish.getPrice())
                        .categoryName(cat.getName())
                        .type("DISH")
                        .flavorOptions(flavorNames)
                        .description(dish.getDescription())
                        .build());
            }
        }

        // 查询套餐分类下的所有在售套餐
        List<Category> mealCategories = categoryMapper.list(2);
        for (Category cat : mealCategories) {
            Setmeal query = Setmeal.builder().categoryId(cat.getId()).status(1).build();
            List<Setmeal> setmeals = setmealMapper.list(query);
            for (Setmeal sm : setmeals) {
                if (effectiveBudget != null && effectiveBudget.compareTo(BigDecimal.ZERO) > 0) {
                    int diners = Math.max(state.getDinerCount() != null ? state.getDinerCount() : 1, 1);
                    BigDecimal perPersonBudget = effectiveBudget.divide(BigDecimal.valueOf(diners), 2,
                            java.math.RoundingMode.HALF_UP);
                    if (sm.getPrice().compareTo(perPersonBudget.multiply(new BigDecimal("1.5"))) > 0) {
                        continue;
                    }
                }

                candidates.add(MealCandidate.builder()
                        .id(sm.getId())
                        .name(sm.getName())
                        .price(sm.getPrice())
                        .categoryName(cat.getName())
                        .type("SETMEAL")
                        .flavorOptions(new ArrayList<>())
                        .description(sm.getDescription())
                        .build());
            }
        }

        state.setCandidates(candidates);
        state.setIteration(state.getIteration() + 1);
        log.info("[searchDishes] 找到 {} 个候选菜品/套餐", candidates.size());
        return state;
    }

    // ==================== 节点 3: 评估 + 生成方案（合并，单次 LLM 调用）====================

    private MealPlanState evaluateAndGenerate(MealPlanState state) {
        log.info("[evaluateAndGenerate] 评估候选菜品并生成方案/松弛约束");

        if (state.getCandidates().isEmpty()) {
            state.setSatisfactionScore(0);
            state.setIssues(List.of("没有找到符合条件的菜品"));
            state.setFinalPlan("抱歉，未能找到符合条件的菜品组合。建议放宽预算或饮食限制后重试。");
            return state;
        }

        String candidatesSummary = buildCandidatesSummary(state.getCandidates());

        int iter = state.getIteration() != null ? state.getIteration() : 0;
        int maxIter = state.getMaxIterations() != null ? state.getMaxIterations() : 3;
        boolean isLastAttempt = iter >= maxIter - 1;

        String prompt = """
                你是智能点餐助手。评估候选菜品是否能满足用户约束，并据此生成推荐方案或建议松弛约束。

                ## 用户约束
                - 用餐人数：%d人
                - 总预算：%s元
                - 忌口/限制：%s
                - 菜品要求：%s

                ## 候选菜品（共%d道）
                %s

                请返回严格JSON（不要其他文字）：
                {
                  "satisfactionScore": <0-100的满足度评分>,
                  "issues": ["问题1", "问题2"],
                  "plan": "<推荐方案文本，含菜品名称、价格、推荐理由、总价。如果无法满足且还有重试机会，设为空字符串>",
                  "relaxedRequirements": ["<松弛后的菜品要求，如果已生成方案则为空数组>"]
                }

                规则：
                - %s：即使满足度不达标也必须生成推荐方案（plan字段非空），relaxedRequirements为空数组
                - %s：plan字段设为空字符串，relaxedRequirements给出1-2条松弛后的要求
                """.formatted(
                        state.getDinerCount() != null ? state.getDinerCount() : 1,
                        state.getBudget() != null ? state.getBudget().toString() : "不限",
                        state.getRestrictions().isEmpty() ? "无" : String.join("、", state.getRestrictions()),
                        state.getRequirements().isEmpty() ? "无" : String.join("、", state.getRequirements()),
                        state.getCandidates().size(),
                        candidatesSummary,
                        isLastAttempt ? "最后一轮尝试" : "如果满足度 >= 80",
                        isLastAttempt ? "" : "如果满足度 < 80 且还有重试机会"
                );

        String json = callLlm(
                "你是一个专业的智能点餐助手，擅长评估菜品搭配并推荐套餐。回答使用中文。",
                prompt
        );

        try {
            JSONObject obj = JSON.parseObject(json);
            state.setSatisfactionScore(obj.getInteger("satisfactionScore"));
            state.setIssues(parseStringList(obj, "issues"));

            String plan = obj.getString("plan");
            if (plan != null && !plan.isEmpty() && !"null".equals(plan)) {
                state.setFinalPlan(plan);
                log.info("[evaluateAndGenerate] 方案已生成，长度: {} 字符", plan.length());
            }

            List<String> relaxed = parseStringList(obj, "relaxedRequirements");
            if (relaxed != null && !relaxed.isEmpty()) {
                state.setRelaxedRequirements(relaxed);
                log.info("[evaluateAndGenerate] 满足度: {}/100, 松弛建议: {}", state.getSatisfactionScore(), relaxed);
            } else {
                log.info("[evaluateAndGenerate] 满足度: {}/100, 问题: {}", state.getSatisfactionScore(), state.getIssues());
            }
        } catch (Exception e) {
            log.warn("[evaluateAndGenerate] JSON解析失败，使用默认评分并尝试提取。原始响应: {}", json, e);
            state.setSatisfactionScore(50);
            state.setIssues(new ArrayList<>());
            // 兜底：尝试从响应中提取非JSON文本作为方案
            if (json != null && !json.isEmpty() && !json.startsWith("{")) {
                state.setFinalPlan(json);
            }
        }

        return state;
    }

    // ==================== 节点 4: 松弛约束（纯状态操作，不调用 LLM）====================

    private MealPlanState relaxConstraints(MealPlanState state) {
        log.info("[relaxConstraints] 应用松弛约束，当前满足度: {}, 迭代: {}",
                state.getSatisfactionScore(), state.getIteration());

        List<String> relaxed = state.getRelaxedRequirements();
        if (relaxed != null && !relaxed.isEmpty()) {
            state.setRequirements(relaxed);
            log.info("[relaxConstraints] 已更新菜品要求: {}", relaxed);
        } else {
            // 兜底：移除最后一个要求
            if (state.getRequirements() != null && !state.getRequirements().isEmpty()) {
                state.getRequirements().remove(state.getRequirements().size() - 1);
                log.info("[relaxConstraints] 兜底：移除最后一个要求，剩余: {}", state.getRequirements());
            }
        }

        return state;
    }

    // ==================== 条件路由 ====================

    private String routeAfterEvaluate(MealPlanState state) {
        String plan = state.getFinalPlan();
        if (plan != null && !plan.isEmpty() && !"null".equals(plan)) {
            log.info("[路由] 方案已生成 → 结束");
            return "generate";
        }

        int iter = state.getIteration() != null ? state.getIteration() : 0;
        int maxIter = state.getMaxIterations() != null ? state.getMaxIterations() : 3;
        if (iter >= maxIter) {
            log.info("[路由] 已达最大循环次数 ({}/{}) → 强制结束", iter, maxIter);
            return "generate";
        }

        log.info("[路由] 未生成方案，迭代{} < {} → 松弛约束后重试", iter, maxIter);
        return "relax";
    }

    // ==================== 辅助方法 ====================

    /** 调用 LLM，返回文本内容（默认不控制 thinking） */
    private String callLlm(String systemPrompt, String userPrompt) {
        return miMoClient.chat(systemPrompt, userPrompt);
    }

    /** 调用 LLM，返回文本内容 */
    private String callLlm(String systemPrompt, String userPrompt, boolean disableThinking) {
        return miMoClient.chat(systemPrompt, userPrompt, disableThinking);
    }

    /** 从 JSONObject 中安全提取字符串列表 */
    private List<String> parseStringList(JSONObject obj, String key) {
        JSONArray arr = obj.getJSONArray(key);
        if (arr == null) return new ArrayList<>();
        return arr.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    /** 构建候选菜品摘要（最多20个，避免token超限） */
    private String buildCandidatesSummary(List<MealCandidate> candidates) {
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(candidates.size(), 20);
        for (int i = 0; i < limit; i++) {
            MealCandidate c = candidates.get(i);
            sb.append(String.format("- [%s] %s（￥%s）分类：%s",
                    c.getType(), c.getName(), c.getPrice().toString(), c.getCategoryName()));
            if (c.getFlavorOptions() != null && !c.getFlavorOptions().isEmpty()) {
                sb.append(" 口味：").append(String.join(",", c.getFlavorOptions()));
            }
            if (c.getDescription() != null && !c.getDescription().isEmpty()) {
                sb.append(" 描述：").append(c.getDescription());
            }
            sb.append("\n");
        }
        if (candidates.size() > limit) {
            sb.append("...还有").append(candidates.size() - limit).append("道菜品未列出\n");
        }
        return sb.toString();
    }
}
