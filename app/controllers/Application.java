package controllers;

import com.google.common.collect.ImmutableMap;
import domains.Manager;
import enums.ErrorCode;
import lib.BizException;
import play.db.jpa.JPA;

import java.util.List;

public class Application extends BasicController {

    public static void index() {
        renderTemplate();
    }

    public static void login(String username, String password) {
        // TODO: 2018/4/18 理论上密码要加密 
        Manager manager = Manager.find("name = ? and password = ?", username, password).first();
        if (manager == null) {
            err(new BizException(ErrorCode._10001));
        }
        String sql = String.format("SELECT usr_name, prc_name, MAX(sign_time),CASE WHEN TIMEDIFF(TIME(MAX(sign_time)), '08:30:00') > 0 THEN '是' ELSE '否' END AS '是否迟到', COUNT(sign_id) AS times FROM (\n" +
                "SELECT u.usr_name, pro.prc_name, s.sign_time, s.id AS sign_id, u.usr_id, s.prc_id FROM sign s LEFT JOIN usr u ON s.usr_id = u.usr_id LEFT JOIN project pro ON s.prc_id = pro.prc_id " +
                "LEFT JOIN privilege p ON p.prc_id = pro.prc_id LEFT JOIN manager m ON m.mng_id = p.mng_id WHERE m.mng_id = %s ORDER BY s.sign_time DESC\n" +
                ") a\n" +
                "GROUP BY usr_name, prc_name", manager.id);

        List<Object[]> list = JPA.em().createNativeQuery(sql).getResultList();

        renderTemplate(ImmutableMap.of("list", list));
    }

}