package com.share.job.util;

import java.util.Date;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.share.common.core.constant.ScheduleConstants;
import com.share.common.core.utils.ExceptionUtil;
import com.share.common.core.utils.SpringUtils;
import com.share.common.core.utils.StringUtils;
import com.share.common.core.utils.bean.BeanUtils;
import com.share.job.domain.SysJob;
import com.share.job.domain.SysJobLog;
import com.share.job.service.ISysJobLogService;

/**
 * 定时任务执行的抽象模板
 *
 * 用了模板方法模式：
 *   execute() → before() → doExecute() → after()（子类实现 doExecute）
 *   before()：记开始时间到 ThreadLocal
 *   after()：从 ThreadLocal 取时间，算耗时，写日志
 *
 * @author share
 */
public abstract class AbstractQuartzJob implements Job
{
    private static final Logger log = LoggerFactory.getLogger(AbstractQuartzJob.class);

    /**
     * ★ 任务开始时间的 ThreadLocal
     *
     * 为什么用 ThreadLocal？
     *   before() 里 set(new Date()) —— 记下"什么时候开始执行"
     *   after()  里 get()            —— 取出开始时间，算执行耗时
     *   after()  里 remove()         —— 用完清理
     *
     * Quartz 的 Job 实例可能被多个线程并发执行（同一时刻多个 trigger 触发同一个 Job 类），
     * 如果开始时间存成实例变量，并发执行时互相覆盖，耗时计算全错。
     * ThreadLocal 保证每个线程（每个任务执行）有自己独立的时间副本。
     */
    private static ThreadLocal<Date> threadLocal = new ThreadLocal<>();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        SysJob sysJob = new SysJob();
        BeanUtils.copyBeanProp(sysJob, context.getMergedJobDataMap().get(ScheduleConstants.TASK_PROPERTIES));
        try
        {
            before(context, sysJob);
            if (sysJob != null)
            {
                doExecute(context, sysJob);
            }
            after(context, sysJob, null);
        }
        catch (Exception e)
        {
            log.error("任务执行异常  - ：", e);
            after(context, sysJob, e);
        }
    }

    /**
     * 执行前
     *
     * ★ 记下当前时间到 ThreadLocal
     * 后续 after() 取出计算执行耗时
     *
     * @param context 工作执行上下文对象
     * @param sysJob 系统计划任务
     */
    protected void before(JobExecutionContext context, SysJob sysJob)
    {
        threadLocal.set(new Date());
    }

    /**
     * 执行后
     *
     * ★ 从 ThreadLocal 取出开始时间，计算执行耗时，写入日志表
     * 最后清理 ThreadLocal（remove）
     *
     * @param context 工作执行上下文对象
     * @param sysJob 系统计划任务
     * @param e 执行过程中的异常（没有则 null）
     */
    protected void after(JobExecutionContext context, SysJob sysJob, Exception e)
    {
        Date startTime = threadLocal.get();
        threadLocal.remove();  // ★ 用完立即清理，防止内存泄漏

        final SysJobLog sysJobLog = new SysJobLog();
        sysJobLog.setJobName(sysJob.getJobName());
        sysJobLog.setJobGroup(sysJob.getJobGroup());
        sysJobLog.setInvokeTarget(sysJob.getInvokeTarget());
        sysJobLog.setStartTime(startTime);
        sysJobLog.setStopTime(new Date());
        long runMs = sysJobLog.getStopTime().getTime() - sysJobLog.getStartTime().getTime();
        sysJobLog.setJobMessage(sysJobLog.getJobName() + " 总共耗时：" + runMs + "毫秒");
        if (e != null)
        {
            sysJobLog.setStatus("1");
            String errorMsg = StringUtils.substring(ExceptionUtil.getExceptionMessage(e), 0, 2000);
            sysJobLog.setExceptionInfo(errorMsg);
        }
        else
        {
            sysJobLog.setStatus("0");
        }

        // 写入数据库当中
        SpringUtils.getBean(ISysJobLogService.class).addJobLog(sysJobLog);
    }

    /**
     * 执行方法，由子类重载
     *
     * @param context 工作执行上下文对象
     * @param sysJob 系统计划任务
     * @throws Exception 执行过程中的异常
     */
    protected abstract void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception;
}
