package com.engine.yunphant.action;


import com.weaver.general.BaseBean;
import com.weaver.general.Util;
import weaver.conn.RecordSet;
import weaver.formmode.customjavacode.AbstractModeExpandJavaCodeNew;
import weaver.soa.workflow.request.RequestInfo;
import weaver.formmode.setup.ModeRightInfo;
import java.util.UUID;

import java.util.HashMap;
import java.util.Map;

public class ModeExpandUpdateAction1 extends AbstractModeExpandJavaCodeNew {
    @Override
    public Map<String, String> doModeExpand(Map<String, Object> param) {
        RecordSet rs = new RecordSet();
        RecordSet rs2 = new RecordSet();
        RecordSet rs3 = new RecordSet(); //查询流水号
        ModeRightInfo ModeRightInfo = new ModeRightInfo();
        int billid = -1;
        int modeid = -1;
        int formmodeid = 30;
        String data_id = null;
        // Map<String,String> result=new HashMap<>();
        Map<String, String> map = new HashMap<>();
        BaseBean baseBean = new BaseBean();
        try {

            RequestInfo requestInfo = (RequestInfo) param.get("RequestInfo");
            if (requestInfo != null) {
                billid = Util.getIntValue(requestInfo.getRequestid());
                modeid = Util.getIntValue(requestInfo.getWorkflowid());
            }
            if (billid > 0 && modeid > 0) {
                //获取表单主表数据
                String sql = "select a.modedatacreater,a.modedatacreatedate,a.modedatacreatetime,b.id,a.modedatastatus from uf_xmbgd_tb a inner join uf_xmbgd_tb_dt1 b on a.id = b.mainid where b.mainid = ? ";
                String sql_lsh = "select * from uf_liushuihao where bs = 13";
                String sql_lsh_add = "update uf_liushuihao set dqls = ifnull(dqls,0) + 1 where bs = 13";
                rs.executeQuery(sql, billid);
               // if(rs.next()){
                    while (rs.next()) {
                        map.put("modedatacreater", rs.getString("modedatacreater"));
                        map.put("modedatacreatedate", rs.getString("modedatacreatedate"));
                        map.put("modedatacreatetime", rs.getString("modedatacreatetime"));
                        map.put("modedatastatus", rs.getString("modedatastatus"));
                        map.put("mxbm", rs.getString("id")); //明细编码UUID
                        map.put("gszt", "1".equals(rs.getString("modedatastatus")) ? "0":"1");
                        //获取流水号
                        rs3.executeUpdate(sql_lsh_add);  //流水号+1
                        rs3.executeQuery(sql_lsh); //获取当前流水号
                        int max = 0;
                        String max2 = "00001";
                        String bgdbh = "MRBS";
                        if(rs3.next()) {
                            max = Integer.parseInt(rs3.getString("dqls"));
                            if(max < 10 && max >0 ){
                                max2 = "0000" + Integer.toString(max);
                            }
                            if(max > 10 && max < 100){
                                max2 = "000" + Integer.toString(max);
                            }
                            if(max >100 && max < 1000){
                                max2 = "00" + Integer.toString(max);
                            }
                            if(max>1000 && max < 10000){
                                max2 = "0" + Integer.toString(max);
                            }
                            if(max > 10000){
                                max2 = Integer.toString(max);
                            }

                            bgdbh = rs3.getString("dz");
                        }
                        bgdbh = bgdbh + map.get("modedatacreatedate").substring(0, 8) + max2;
                        //生成UUID
                        UUID uuid = UUID.randomUUID();
                        String modeuuid = UUID.randomUUID().toString();
                        //插入数据到审核表中
                        String insert_history = "insert into uf_baogongdanshenhe(formmodeid,mxbm,gszt,modeuuid,bgdbh) values(?,?,?,?,?)";
                        rs.executeUpdate(insert_history,formmodeid,map.get("mxbm"),map.get("gszt"),modeuuid,bgdbh);
                        //更新审核表中数据
                        String sql_update = "update uf_baogongdanshenhe a,(select a.modedatacreater,a.modedatacreatedate,a.modedatacreatetime,a.modedatamodifier,a.modedatamodifydatetime,a.sqrq,a.sqr,a.sqbm,a.modedatastatus,a.gszt,b.xmbh,b.xmmc,b.xmlx,b.xmjl,b.xmszbm,b.rq,b.gs,b.gzlx,b.bzjb,b.gzyfxm,b.bzje,b.sfbzje,b.gzsx,b.id mxid,b.mxbm,b.xmfzr,b.gzyfxmmc,b.gzyfxmjl,b.gzyfxmfzr from uf_xmbgd_tb a inner join uf_xmbgd_tb_dt1 b on a.id = b.mainid where b.id = ?) t set a.modedatacreater = t.modedatacreater,a.modedatacreatedate = t.modedatacreatedate,a.modedatacreatetime = t.modedatacreatetime,a.modedatamodifier = t.modedatamodifier,a.modedatamodifydatetime = t.modedatamodifydatetime,a.sqrq = t.sqrq,a.sqr = t.sqr,a.sqbm = t.sqbm,a.xmbh = t.xmbh,a.xmmc = t.xmmc,a.xmlx = t.xmlx,a.xmjl = t.xmjl,a.xmszbm = t.xmszbm,a.rq = t.rq,a.gs = t.gs,a.gzsx = t.gzsx,a.gzlx = t.gzlx,a.bzjb = t.bzjb,a.bzje = t.bzje,a.sfbzje = t.sfbzje,a.gzyfxm = t.gzyfxm,a.modedatastatus = t.modedatastatus,a.xmfzr = t.xmfzr,a.gzyfxmmc = t.gzyfxmmc,a.gzyfxmjl = t.gzyfxmjl,a.gzyfxmfzr = t.gzyfxmfzr,a.ycgsdz = 0 where a.mxbm = t.mxid";
                        rs.executeUpdate(sql_update,map.get("mxbm"));
                        //查询新插入的数据ID
                        String sql_new = "select * from uf_baogongdanshenhe where modeuuid = ?";
                        rs2.executeQuery(sql_new,modeuuid);
                        if(rs2.next()){
                            data_id = rs2.getString("id");
                        }

                        ModeRightInfo.setNewRight(true); //新建数据插入数据权限
                        ModeRightInfo.editModeDataShare(Integer.parseInt(map.get("modedatacreater")),formmodeid,Integer.parseInt(data_id)); //执行该数据的数据权限重构

                        baseBean.writeLog("map：" + map.get("MODEUUID") + "，插入数据的UUID：" + modeuuid + "，插入后数据ID：" + data_id + "，创建人：" + map.get("modedatacreater") + "流水号：" + bgdbh);
                       // baseBean.writeLog(insert_history);
                    }
                baseBean.writeLog(sql);
               // }
            }
        } catch (Exception e) {
            map.put("errmsg", "插入数据执行错误");
            map.put("flag", "false");
            baseBean.writeLog(e);
            baseBean.writeLog(map.get("errmsg"));
        }
        return map;
    }
}