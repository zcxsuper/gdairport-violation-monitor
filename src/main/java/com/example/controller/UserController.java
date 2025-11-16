package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.example.annotation.AuthorizeAdmin;
import com.example.domain.ResponseResult;
import com.example.domain.ResultCode;
import com.example.domain.dto.LoginDto;
import com.example.domain.dto.RegisterDto;
import com.example.domain.dto.UserUpdateDto;
import com.example.domain.entity.User;
import com.example.domain.vo.LoginVo;
import com.example.domain.vo.UserInfoVo;
import com.example.service.UserService;
import com.example.util.JwtUtil;
import com.example.util.TokenRedisUtil;
import com.example.util.UserContextUtil;
import lombok.RequiredArgsConstructor;
import com.example.exception.BadRequestException;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final TokenRedisUtil tokenRedisUtil;

    /**
     * <p>
     * 用户登陆注册，前端控制器
     * </p>
     *
     * @author vlsmb
     */
    @Value("${user.password-length}")
    private int passwordLength;

    /**
     * 注册
     *
     * @param registerDto
     * @return
     * @throws BadRequestException
     */
    @PostMapping("/register")
    public ResponseResult<String> register(@Validated @RequestBody RegisterDto registerDto) {
        // 字符串去掉左侧右侧空格
        String password = registerDto.getPassword().trim();
        String confirmPassword = registerDto.getConfirmPassword().trim();

        // 检验密码是否符合规则
        if (!password.equals(confirmPassword)) {
            throw new BadRequestException("两次密码不一致");
        }
        if (password.length() < passwordLength) {
            throw new BadRequestException("密码的长度至少为" + passwordLength + "位！");
        }
        // 查询该邮箱是否注册
        if (userService.findEnabledUserId(registerDto.getEmail()) != null) {
            throw new BadRequestException("该邮箱已注册");
        }
        return ResponseResult.success(userService.register(registerDto));
    }

    /**
     * 登录
     *
     * @param loginDto
     * @return
     */
    @PostMapping("/login")
    public ResponseResult<LoginVo> login(@Validated @RequestBody LoginDto loginDto) {
        User user = userService.checkPassword(loginDto);
        if (user == null) {
            return ResponseResult.error(ResultCode.UNAUTHORIZED, "用户不存在或者密码错误");
        }
        String token = jwtUtil.generateToken(user.getId());
        tokenRedisUtil.addToken(user.getId(), token);
        return ResponseResult.success(LoginVo.builder().userId(user.getId()).token(token).build());
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    public ResponseResult<T> logout() {
        Long userId = UserContextUtil.getUserId();
        tokenRedisUtil.removeToken(userId);
        return ResponseResult.success();
    }

    /**
     * 查看个人信息
     *
     * @return
     */
    @GetMapping
    public ResponseResult<UserInfoVo> getUserInfo() {
        Long userId = UserContextUtil.getUserId();
        User user = userService.getById(userId);
        if (user == null) {
            return ResponseResult.error(ResultCode.BAD_REQUEST, "用户信息不存在");
        }
        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtils.copyProperties(user, userInfoVo);
        return ResponseResult.success(userInfoVo);
    }

    /**
     * 注销
     *
     * @return
     */
    @DeleteMapping
    public ResponseResult<T> cancelUser() {
        Long userId = UserContextUtil.getUserId();
        // 执行注销操作
        userService.removeById(userId);
        // 使Token失效
        tokenRedisUtil.removeToken(userId);
        return ResponseResult.success();
    }

    /**
     * 修改个人信息
     *
     * @param userUpdateDto
     * @return
     */
    @PutMapping
    public ResponseResult<Object> updateUserInfo(@RequestBody UserUpdateDto userUpdateDto) {
        Long userId = UserContextUtil.getUserId();
        userService.updateUserInfo(userId, userUpdateDto);
        return ResponseResult.success();
    }

    ////// 管理员额外操作 //////

    /**
     * 删除用户
     *
     * @return
     */
    @DeleteMapping("/admin/{id}")
    @AuthorizeAdmin
    public ResponseResult<T> deleteUser(@PathVariable Long id) {
        Long userId = UserContextUtil.getUserId();
        // 执行删除操作
        userService.removeUser(userId, id);
        // 使Token失效
        tokenRedisUtil.removeToken(id);
        return ResponseResult.success();
    }

    @GetMapping("/admin/email")
    @AuthorizeAdmin
    public ResponseResult<User> getUserByEmail(@RequestParam String email) {
        return ResponseResult.success(userService.getUserByEmail(email));
    }

    /**
     * 查看用户列表
     */
    @GetMapping("/admin")
    @AuthorizeAdmin
    public ResponseResult<PageDTO<UserInfoVo>> getAllUserInfo(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                              @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        //分页查询订单信息
        PageDTO<UserInfoVo> page = userService.getAllUserInfo(pageNum, pageSize);
        return ResponseResult.success(page);
    }
}
