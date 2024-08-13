package com.tell.controller;

import com.tell.bean.Page;
import com.tell.conf.properties.SiteConfig;
import com.tell.bean.Result;
import com.tell.exception.ApiAssert;
import com.tell.model.AccessToken;
import com.tell.model.Article;
import com.tell.model.User;
import com.tell.service.AccessTokenService;
import com.tell.service.ArticleService;
import com.tell.service.ThemeService;
import com.tell.service.UserService;
import com.tell.util.JwtTokenUtil;
import com.tell.util.StringUtil;
import com.tell.util.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

/**
 * @Author: miansen
 * @Date: 2018/11/17 15:23
 */
@RestController
public class IndexController {

    @Autowired
    private ArticleService articleService;
    @Autowired
    private UserService userService;
    @Autowired
    private AccessTokenService accessTokenService;
    @Autowired
    private ThemeService themeService;
    @Autowired
    private SiteConfig siteConfig;
    @Autowired
    private StringUtil stringUtil;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    /**
     * 24小时
     * @return
     */
    @GetMapping(value = "/articles")
    private Result articles(@RequestParam(value = "pageNo",defaultValue = "1") Integer pageNo){
        Page<Article> page = articleService.page(pageNo, siteConfig.getPageSize());
        return Result.success(page);
    }

    /**
     * 热门文章
     * @return
     */
    @GetMapping(value = "/articles/hot")
    private Result hotNews(@RequestParam(value = "pageNo",defaultValue = "1") Integer pageNo,
                           @RequestParam(value = "pageSize",defaultValue = "5") Integer pageSize){
        Page<Article> page = articleService.pageByWeight(pageNo, pageSize);
        return Result.success(page);
    }

    @PostMapping(value = "/register")
    public Result register(@RequestBody User user){
        String username = user.getUsername();
        String password = user.getPassword();
        String email = user.getEmail();
        ApiAssert.notEmpty(username, "请输入用户名");
        ApiAssert.notEmpty(password, "请输入密码");
        ApiAssert.isTrue(stringUtil.check(username,stringUtil.usernameRegex),"用户名只能输入[0-9a-zA-Z]，长度4-16位");
        ApiAssert.isTrue(stringUtil.check(password,stringUtil.passwordRegex),"密码只能输入[0-9a-zA-Z]，长度6-32位");
        user = userService.findUserByName(username);
        ApiAssert.isNull(user,"用户名已经存在");
        // 保存用户
        user = userService.create(username, password, email);
        // 保存Token
        AccessToken accessToken = accessTokenService.create(jwtTokenUtil.generateToken(user.getUsername()), user.getUserId());
        HashMap<String, Object> map = new HashMap<>();
        map.put("username",user.getUsername());
        map.put("avatar",user.getAvatar());
        map.put("token",accessToken.getToken());
        return Result.success(map);
    }

    @PostMapping(value = "/login")
    public Result login(@RequestBody User user){
        String username = user.getUsername();
        String password = user.getPassword();
        ApiAssert.notEmpty(username, "请输入用户名");
        ApiAssert.notEmpty(password, "请输入密码");
        user = userService.findUserByName(username);
        ApiAssert.notNull(user, "用户不存在");
        // ApiAssert.isTrue(new BCryptPasswordEncoder().matches(password, user.getPassword()), "密码不正确");
        // 使用BCrypt验证密码
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        boolean isPasswordValid = passwordEncoder.matches(password, user.getPassword());
        if (!isPasswordValid) {
            return Result.error("密码不正确");
        }
        AccessToken accessToken = accessTokenService.getByUserId(user.getUserId());
        HashMap<String, Object> map = new HashMap<>();
        map.put("username",user.getUsername());
        map.put("avatar",user.getAvatar());
        map.put("token",accessToken.getToken());
        return Result.success(map);
    }
}
