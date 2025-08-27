package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    //微信服务接口地址
    private static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    /**
     * 微信登录
     *
     * @param userLoginDTO
     * @return
     */
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        String openid = getOpenid(userLoginDTO.getCode());

        //3.判断OpenId是否为空
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //4.判断当前用户是否为新用户
        User user = userMapper.getByOpenId(openid);
        //5.如果是新用户，自动完成注册
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        //6.返回这个用户对象
        return user;
    }

    /**
     * 调用微信接口服务,获取微信用户openid
     *
     * @param code
     * @return
     */
    public String getOpenid(String code) {
        //1.调用微信服务器接口，并接收一下结果
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
            map.put("grant_type", "authorization_code");
            HttpClientUtil.doGet(WX_LOGIN_URL, map);
            try {
                String json = HttpClientUtil.doGet(WX_LOGIN_URL, map);
            //2.解析微信接口返回的json字符串，获取openid
            JSONObject jsonObject = JSON.parseObject(json);
            // 检查是否有错误码
            if (jsonObject.containsKey("errcode")) {
                Integer errcode = jsonObject.getInteger("errcode");
                String errmsg = jsonObject.getString("errmsg");
                log.error("微信接口调用失败，错误码: {}, 错误信息: {}", errcode, errmsg);

                // code已被使用或其他错误，返回null
                return null;
            }

            // 正常情况返回openid
            return jsonObject.getString("openid");
        } catch (Exception e) {
            log.error("调用微信接口异常", e);
            return null;
        }
    }


}
