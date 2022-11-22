package com.engine.yunphant.action;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.RequestInfo;

/**
 * @Auther: ~zZ
 * @Date: ：2020-10-26
 * @Description:控制明细表发票号码、发票代码不允许重复
 *
 */

public class ApprovalInterceptionInvoice extends BaseBean implements Action{
    public String execute(RequestInfo info) {

        try {
            RecordSet mainrs = new RecordSet();
            RecordSet mainrs2 = new RecordSet();
            String requestid = info.getRequestid(); //获取流程请求ID
            //String tablename = DateUtil.getTablename(info); //获取表单名称
            StringBuilder sql = new StringBuilder();
            StringBuilder sql2 = new StringBuilder();
            sql.append("select * from (select COUNT(*) sl,b.fphm,b.fpdm from formtable_main_16 a inner join formtable_main_16_dt1 b on a.id = b.mainid where b.sfdzfp = 0 and a.requestid = ? group by REPLACE(b.fpdm ,' ',''),REPLACE(b.fphm ,' ','')) a where a.sl > 1");//判断当前表单中是否有重复
            sql2.append("select 1 sl,fphm,fpdm from (select REPLACE(b.fpdm ,' ','') fphm,REPLACE(b.fphm ,' ','') fpdm,ifnull(c.mainid,0) bs from \n" +
                    "formtable_main_16 a inner join formtable_main_16_dt1 b on a.id = b.mainid and a.requestid = ? and b.sfdzfp = 0\n" +
                    "left join formtable_main_16_dt1 c on REPLACE(b.fpdm ,' ','') = REPLACE(c.fpdm ,' ','') and \n" +
                    "REPLACE(b.fphm ,' ','') = REPLACE(c.fphm ,' ','') and b.mainid <> c.mainid and c.sfdzfp = 0) a where a.bs <> 0"); //判断其他表单是否有重复
            mainrs.executeQuery(sql.toString(),requestid); //查询返回结果
            mainrs2.executeQuery(sql2.toString(),requestid); //查询返回结果
            writeLog("sql==" + sql); // 输出查询SQL日志
            writeLog("sql2==" + sql2);
            //writeLog("mainrs.next()=" + mainrs.next());
                while (mainrs.next()) {
                   // writeLog("进入循环");
                    String sl = Util.null2String(mainrs.getString("sl")); //获取当前行数量
                    String fphm = Util.null2String(mainrs.getString("fphm")); //发票号码
                    String fpdm = Util.null2String(mainrs.getString("fpdm")); //发票代码
                    writeLog("sl="+sl);
                    if(Integer.parseInt(sl) > 1){ //如果数量大于1，说明当前填报的明细表中有重复的发票号码和代码
                        info.getRequestManager().setMessageid("1000");
                        info.getRequestManager().setMessagecontent("当前表单中的的发票号码：" + fphm.replaceAll(" ", "") + "，发票代码：" + fpdm.replaceAll(" ", "") + ",存在重复。同一发票号码与发票代码不允许重复使用！");
                        return FAILURE_AND_CONTINUE;
                    }
                }
                while (mainrs2.next())  {
                    String sl2 = Util.null2String(mainrs2.getString("sl")); //获取当前行数量
                    String fphm2 = Util.null2String(mainrs2.getString("fphm")); //发票号码
                    String fpdm2 = Util.null2String(mainrs2.getString("fpdm")); //发票代码
                    writeLog("sl2="+sl2);
                    if(Integer.parseInt(sl2) > 0){
                        info.getRequestManager().setMessageid("1000");
                        info.getRequestManager().setMessagecontent("当前表单中的的发票号码：" + fphm2.replaceAll(" ", "") + "，发票代码：" + fpdm2.replaceAll(" ", "") + ",在其他报销中已经被使用！同一发票号码与发票代码不允许重复使用！");
                        return FAILURE_AND_CONTINUE;
                    }
                }
            return SUCCESS;
        } catch (Exception e) {
            writeLog("e="+e);
            info.getRequestManager().setMessageid("10000");
            info.getRequestManager().setMessagecontent("出现未知错误，请联系管理员！");
            return FAILURE_AND_CONTINUE;
        }
    }
}