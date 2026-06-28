package com.share.auth.form;

/**
 * 用户登录对象
 *
 * 前端登录页提交的请求体，
 * 浏览器 POST /auth/login 时把 JSON 转成这个 Java 对象。
 *
 * 前端发送的 JSON：
 * {
 *   "username": "admin",
 *   "password": "admin123"
 * }
 *
 * Spring 的 @RequestBody 注解自动把 JSON 的字段名
 * 跟这个类的属性名匹配，调用 setter 方法赋值。
 *
 * @author share
 */
public class LoginBody
{
    /**
     * 用户名
     * 对应前端 login.vue 中 loginForm.username 的值
     */
    private String username;

    /**
     * 用户密码
     * 对应前端 login.vue 中 loginForm.password 的值
     * 注意：密码是以明文传输的，所以生产环境必须走 HTTPS
     */
    private String password;

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }
}
