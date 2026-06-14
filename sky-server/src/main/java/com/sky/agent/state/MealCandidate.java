package com.sky.agent.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 候选菜品/套餐模型
 * searchDishes 节点查询数据库后，将可用菜品封装为此对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealCandidate {

    /** 菜品/套餐 ID */
    private Long id;

    /** 名称 */
    private String name;

    /** 单价 */
    private BigDecimal price;

    /** 所属分类名称 */
    private String categoryName;

    /** 类型：DISH（单品）或 SETMEAL（套餐） */
    private String type;

    /** 可选口味列表（如：["不辣","微辣","中辣","重辣"]），套餐此项为空 */
    private List<String> flavorOptions;

    /** 描述信息 */
    private String description;
}
