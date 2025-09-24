sky-take-out 外卖点餐系统
项目简介
sky-take-out 是一个基于Spring Boot的外卖点餐系统，包含管理端和用户端两个主要模块。系统实现了完整的外卖业务流程，包括菜品管理、套餐管理、订单处理、支付集成等功能。
技术架构
后端框架: Spring Boot + MyBatis
数据库: MySQL
缓存: Redis
消息通信: WebSocket
文件存储: 阿里云OSS
支付集成: 微信支付
安全认证: JWT
API文档: Knife4j
模块结构
1. sky-pojo (数据对象模块)
   包含项目中使用的数据传输对象(DTO)、实体类(Entity)和视图对象(VO)。
2. sky-common (通用模块)
   包含项目通用的常量、异常处理、工具类、配置属性等。
3. sky-server (服务模块)
   项目核心服务模块，包含控制器、业务逻辑、数据访问等完整业务实现。
   核心功能
   管理端功能
   员工管理(登录、信息维护)
   分类管理(菜品分类、套餐分类)
   菜品管理(菜品信息、口味设置)
   套餐管理(套餐信息、关联菜品)
   订单管理(订单处理、状态跟踪)
   数据统计(营业额、用户量、销售排行)
   店铺设置(营业状态控制)
   用户端功能
   微信登录
   菜品浏览
   购物车管理
   订单提交
   订单查询
   地址管理
   微信支付
   主要技术组件
   数据库实体
   Employee: 员工信息
   Category: 分类信息
   Dish: 菜品信息
   Setmeal: 套餐信息
   Orders: 订单信息
   User: 用户信息
   AddressBook: 地址簿
   ShoppingCart: 购物车
   核心服务
   EmployeeService: 员工管理服务
   CategoryService: 分类管理服务
   DishService: 菜品管理服务
   SetmealService: 套餐管理服务
   OrderService: 订单管理服务
   UserService: 用户管理服务
   工具类
   JwtUtil: JWT工具类
   WeChatPayUtil: 微信支付工具类
   AliOssUtil: 阿里云OSS工具类
   HttpClientUtil: HTTP客户端工具类
   配置说明
   系统通过 application.yml 和 application-dev.yml 进行配置:
   数据库连接配置
   Redis配置
   阿里云OSS配置
   微信支付配置
   JWT安全配置
   部署说明
   初始化数据库，执行数据库脚本
   配置 application-dev.yml 中的相关参数
   编译项目: mvn clean package
   运行项目: java -jar sky-server.jar
   注意事项
   系统需要配置微信支付相关参数才能使用支付功能
   阿里云OSS配置用于文件上传功能
   Redis用于缓存和会话管理
   需要配置JWT密钥保证系统安全