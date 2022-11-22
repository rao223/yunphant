package com.engine.yunphant.action;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.RequestInfo;

/**
 * @Auther: ~zZ
 * @Date: ：2020-10-21
 * @Description:控制提交的当日工时合计必须为8小时。
 *
 */

public class ApprovalInterception extends BaseBean implements Action{
    public String execute(RequestInfo info) {

        try {
            RecordSet mainrs = new RecordSet();
            String requestid = info.getRequestid(); //获取流程请求ID
            //String tablename = DateUtil.getTablename(info); //获取表单名称

            StringBuilder sql = new StringBuilder();
            sql.append("select a.rq,a.sqr,a.tbgs,b.lsgs,ifnull(a.tbgs,0) + ifnull(b.lsgs,0) ljgs from (select b.rq,sum(b.gs) tbgs,a.sqr from formtable_main_116 a" +
                    " inner join formtable_main_116_dt1 b on a.id = b.mainid where a.requestid = ? and b.gzrpd in (0,2) group by b.rq,a.sqr) a " +
                    "left join (select sqr,rq,sum(gs) lsgs from uf_baogongdanshenhe group by sqr,rq) b on a.sqr = b.sqr and a.rq = b.rq");
            writeLog("XMGSTBsql==" + sql); // 输出查询SQL日志
            mainrs.executeQuery(sql.toString(), requestid); //查询返回结果
            while (mainrs.next()) {
                String rq = Util.null2String(mainrs.getString("rq")); //获取日期
                String lsgs = Util.null2String(mainrs.getString("ljgs")); //累计工时

                if("8".equals(lsgs)){ //判断累计工时是否等于8
                    }else{
                        info.getRequestManager().setMessageid("1000");
                        info.getRequestManager().setMessagecontent(rq + "填报总工时为：" + lsgs + "，每日填报工时总计必须等于8小时！");
                        return FAILURE_AND_CONTINUE;
                    }
                // writeLog("fgsjb= " + fgsjb);
                // writeLog("sqje= " + sqje);
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