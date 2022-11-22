package com.engine.yunphant.action;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.RequestInfo;
import java.util.HashMap;

/**
 * 用于云象工时填报流程提交校验
 * 1、判断工作日填报工时必须为8小时
 * 2、判断非工作日填报工时必须为0，同时休息日当天仅允许填写一条数据
 * 3、防止有新的工作类型，但是在补助标准里面没有维护
 * 4、更新星期与工作日的判断，防止表单JS出现错误，导致数据异常
 * 5、更新补贴金额，防止表单JS出现错误，导致数据异常
 */
public class ApprovalInterception2 extends BaseBean implements Action{
    /**
     * 流程路径节点后选择aciton后,会在节点提交后执行此方法。
     */
    public String execute(RequestInfo request) {
        try{
            String requestId = request.getRequestid();
            String tablename = request.getRequestManager().getBillTableName();
            HashMap<String,String> map = new HashMap<>();
            /**
             * 次数、历史次数（cs，lscs，ljcs）：周末需要判断行数（累计行）是否大于1
             * 星期判断（xq）：周日为0
             * 工作日判断（gzrpd）：0 正常工作日、1 公共假日、2 调配工作日、3 调配休息日
             */
            map = validationRules(tablename,requestId);
            String flag = map.get("flag");
            String message = map.get("message");
            if("false".equals(flag)){
                request.getRequestManager().setMessageid("1002");
                request.getRequestManager().setMessagecontent(message);
                writeLog("不允许提交");
                return FAILURE_AND_CONTINUE;
            }else{
                writeLog("允许提交");
                updateData(tablename,requestId);
                return SUCCESS;
            }
        } catch (Exception e) {
            writeLog("Error="+e);
            request.getRequestManager().setMessageid("10000");
            request.getRequestManager().setMessagecontent("出现未知错误，请联系管理员！");
            return FAILURE_AND_CONTINUE;
        }
    }

    public HashMap<String,String> validationRules(String tablename,String requestId){
        RecordSet rs = new RecordSet();
        StringBuilder message = new StringBuilder(); // 错误信息
        HashMap<String,String> map = new HashMap<>();
        StringBuilder sql = new StringBuilder();

        sql.append("select a.requestid,a.sqr"); //请求ID，申请人
        sql.append(",b.rq,ifnull(sum(b.gs),0) tbgs,ifnull(f.lsgs,0) lsgs,ifnull(sum(b.gs),0) + ifnull(f.lsgs,0) ljgs"); // 工作日期，工时合计
        sql.append(",b.gzlx,b.bzjb,e.mrbz,ifnull(e.mrbz,-1) mrbzyc"); // 工作类型、每日标准,每日标准异常数据
        sql.append(",ifnull(count(b.id),0) cs,ifnull(f.lscs,0) lscs,ifnull(count(b.id),0) + ifnull(f.lscs,0) ljcs"); // 次数，历史次数,累计次数
        // sql.append(",c.id groupmemberid,ifnull(c.groupid,1) groupid");// 考勤组信息
        sql.append(",DATE_FORMAT(b.rq, \"%w\" ) xq");// 星期
        sql.append(",case ifnull(d.changeType,0) when 0 then (CASE DATE_FORMAT(b.rq, \"%w\" ) WHEN 0 THEN 1 WHEN 6 THEN 1 ELSE 0 END ) else d.changeType end gzrpd");
        sql.append(" from ");
        sql.append(tablename);
        sql.append(" a inner join ");
        sql.append(tablename);
        sql.append("_dt1 b on a.id = b.mainid");
        sql.append(" left join (select max(id) id ,typevalue,groupid from kq_groupmember where type = 1 AND ( isDelete <> 1 OR isDelete IS NULL ) group by typevalue) c on a.sqr = c.typevalue");
        sql.append(" left join KQ_HolidaySet d on ifnull(c.groupid,1) = d.groupid and b.rq = d.holidaydate");
        sql.append(" left join uf_butiebiaozhun e on b.bzjb = e.gzlx and b.gzlx = e.rwlx");
        sql.append(" left join (select sqr,rq,sum(gs) lsgs,count(*) lscs from uf_baogongdanshenhe group by sqr,rq) f on a.sqr = f.sqr and b.rq = f.rq");
        sql.append(" where a.requestid = ?");
        sql.append(" group by a.sqr,b.rq");

        String flag = "true";
        rs.executeQuery(sql.toString(),requestId);//查询数据
        writeLog("ApprovalInterception_start requestId：" + requestId);
        writeLog("ApprovalInterception_sql："+sql.toString());

        while(rs.next()){
            String sqr = rs.getString("sqr"); // 申请人
            String rq = rs.getString("rq"); // 日期
            String tbgs = rs.getString("tbgs");  // 填报工时
            String lsgs = rs.getString("lsgs");  // 历史工时
            String ljgs = rs.getString("ljgs"); // 累计工时
            String xq = rs.getString("xq"); // 星期
            String gzrpd = rs.getString("gzrpd"); // 工作日判断
            String cs = rs.getString("cs"); // 次数
            String mrbzyc = rs.getString("mrbzyc");// 每日标准异常
            String lscs = rs.getString("lscs"); // 历史次数
            String ljcs = rs.getString("ljcs"); // 累计次数

            if("0".equals(gzrpd) || "2".equals(gzrpd)){ //工作日
                if("8.0".equals(ljgs)){ // 判断工时等于8小时
//                    if("-1".equals(mrbzyc)){ // 工时补贴数据出现异常，需要维护“云象补贴标准”数据。
//                        flag = "false" ; // 设置状态为错误
//                        message.append("填报日期：");
//                        message.append(rq);
//                        message.append("的任务类型、工作类型没有维护补贴标准！请及时联系管理员进行设置！");
//                    }
                } else{
                    flag = "false" ; // 设置状态为错误
                    message.append("填报日期：");
                    message.append(rq);
                    message.append(",填报总工时为：");
                    message.append(ljgs);
                    message.append("小时，每日填报工时总计必须等于8小时！");
                }
            } else{ // 休息日
                if("1".equals(ljcs)){ // 节假日只能发起一次
//                    if("0".equals(ljgs)){ // 节假日填报工时必须为0
//                        flag = "false" ; // 设置状态为错误
//                        message.append("填报日期：");
//                        message.append(rq);
//                        message.append("为休息日，节假日仅填报工时必须为0！请删除对应日期的数据重新填写！");
//                    }
                } else{
                    flag = "false" ; // 设置状态为错误
                    message.append("填报日期：");
                    message.append(rq);
                    message.append("为休息日，节假日仅允许有一条报工数据！");
                }
            }
        }
        map.put("flag",flag);
        map.put("message",message.toString());
        writeLog("ApprovalInterception_end flag = " + flag);
        return map;
    }

    public void updateData(String tablename,String requestId){
        RecordSet rs = new RecordSet();
        StringBuilder sql = new StringBuilder();
        StringBuilder sql1 = new StringBuilder();
        StringBuilder sql2 = new StringBuilder();
        StringBuilder sql3 = new StringBuilder();

        //-----------更新星期和工作日判断信息-------------
        sql.append("update ");
        sql.append(tablename);
        sql.append(" a inner join ");
        sql.append(tablename);
        sql.append("_dt1 b on a.id = b.mainid");
        sql.append(" left join (select max(id) id ,typevalue,groupid from kq_groupmember where type = 1 AND ( isDelete <> 1 OR isDelete IS NULL ) group by typevalue) c on");// 考勤组
        sql.append(" a.sqr = c.typevalue"); //考勤组关联条件（只考虑人力资源，否则就找默认考勤组）
        sql.append(" left join KQ_HolidaySet d on");// 假期设置
        sql.append(" ifnull(c.groupid,1) = d.groupid and b.rq = d.holidaydate");// 假期设置关联条件
        sql.append(" set b.xq = DATE_FORMAT(b.rq, \"%w\" )"); // 更新星期
        sql.append(",b.gzrpd = case ifnull(d.changeType,0) when 0 then (CASE DATE_FORMAT(b.rq, \"%w\" ) WHEN 0 THEN 1 WHEN 6 THEN 1 ELSE 0 END ) else d.changeType end"); // 更新工作日判断
        sql.append(" where a.requestid = ? "); // 更新条件
        //------------如果是休息日更新工时为0--------------
        sql1.append("update ");
        sql1.append(tablename);
        sql1.append(" a inner join ");
        sql1.append(tablename);
        sql1.append("_dt1 b on a.id = b.mainid");
        sql1.append(" set b.gs = 0"); //更新工时为0
        sql1.append(" where b.gzrpd in (1,3) and a.requestid = ? "); // 更新条件
        //------------更新工时补助金额--------------
        sql2.append("update ");
        sql2.append(tablename);
        sql2.append(" a inner join ");
        sql2.append(tablename);
        sql2.append("_dt1 b on a.id = b.mainid");
        sql2.append(" left join uf_butiebiaozhun c"); // 云象补贴标准
        sql2.append(" on b.bzjb = c.gzlx and b.gzlx = c.rwlx"); // 补贴标准匹配条件
        sql2.append(" set b.bzje = case b.gzrpd when 0 then b.gs/8*c.mrbz when 2 then b.gs/8*c.mrbz"); // 工作日补贴标准
        sql2.append(" else c.mrbz end"); // 节假日补贴标准
        sql2.append(" where b.gzlx in (0,13) and a.requestid = ? "); // 更新条件,需判断类型不是请假
        //------------如果是请假更新补贴金额为0--------------
        sql3.append("update ");
        sql3.append(tablename);
        sql3.append(" a inner join ");
        sql3.append(tablename);
        sql3.append("_dt1 b on a.id = b.mainid");
        sql3.append(" set b.bzje = 0"); //更新工时为0
        sql3.append(" where b.gzlx not in (0,13,14,15,16,17,18) and a.requestid = ? "); // 更新条件


        rs.executeUpdate(sql.toString(),requestId); // 更新星期和工作日判断信息
        rs.executeUpdate(sql1.toString(),requestId); // 如果是休息日更新工时为0
        rs.executeUpdate(sql2.toString(),requestId); // 更新工时补助金额
        rs.executeUpdate(sql3.toString(),requestId); // 如果是请假更新补贴金额为0
        writeLog("ApprovalInterception_update SQL："+sql.toString());
        writeLog("ApprovalInterception_update SQL1："+sql1.toString());
        writeLog("ApprovalInterception_update SQL2："+sql2.toString());
        writeLog("ApprovalInterception_update SQL3："+sql3.toString());

        writeLog("ApprovalInterception_update 更新成功");

    }
}