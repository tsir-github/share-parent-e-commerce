package com.share.common.core.utils;

import com.github.pagehelper.PageHelper;
import com.share.common.core.utils.sql.SqlUtil;
import com.share.common.core.web.page.PageDomain;
import com.share.common.core.web.page.TableSupport;

/**
 * 分页工具类
 *
 * @author share
 */
public class PageUtils extends PageHelper
{
    /**
     * 设置请求分页数据
     */
    public static void startPage()//在查询数据库前初始化分页参数，使得后续的 MyBatis 查询自动实现物理分页。
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();//. 获取分页参数,从 请求上下文（如 HTTP 请求参数）或 线程局部变量（如 ThreadLocal）中获取分页参数。
        Integer pageNum = pageDomain.getPageNum();//PageDomain：封装了分页信息的对象，通常包含字段
        Integer pageSize = pageDomain.getPageSize();

        //对排序字段进行 注入防护处理，例如：
        //过滤非法字符（如 ;、--）
        //验证字段名合法性（防止 ORDER BY delete database等攻击）
        String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());

        Boolean reasonable = pageDomain.getReasonable();
        //​​PageHelper.startPage()​​：这是 MyBatis 分页插件的核心方法，作用是为 ​​下一次 SQL 查询​​ 添加分页逻辑
        //自动生成 LIMIT pageSize OFFSET (pageNum-1)*pageSize（MySQL）
        //自动计算总记录数（count查询）
        PageHelper.startPage(pageNum, pageSize, orderBy).setReasonable(reasonable);
        //​​setReasonable()​​：启用分页合理化：
        //若 pageNum <= 0，自动重置为 1
        //若 pageNum > 总页数，自动重置为最大页数
    }

    /**
     * 清理分页的线程变量
     */
    public static void clearPage()
    {
        PageHelper.clearPage();
    }
}
