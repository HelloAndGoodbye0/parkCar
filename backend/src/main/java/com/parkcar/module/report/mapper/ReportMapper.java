package com.parkcar.module.report.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 报表聚合查询
 */
@Mapper
public interface ReportMapper {

    /**
     * 按日/月/年营收汇总（已支付订单）
     */
    @Select("SELECT DATE_FORMAT(create_time, '${fmt}') AS `date`, " +
            "COUNT(*) AS orderCount, COALESCE(SUM(paid_amount), 0) AS amount " +
            "FROM billing_order " +
            "WHERE status = 1 AND deleted = 0 " +
            "AND create_time >= #{start} AND create_time <= #{end} " +
            "GROUP BY DATE_FORMAT(create_time, '${fmt}') ORDER BY `date`")
    List<Map<String, Object>> revenueByDate(@Param("start") String start,
                                            @Param("end") String end,
                                            @Param("fmt") String fmt);

    /**
     * 按支付方式汇总
     */
    @Select("SELECT pay_type, COUNT(*) AS orderCount, COALESCE(SUM(paid_amount), 0) AS amount " +
            "FROM billing_order " +
            "WHERE status = 1 AND deleted = 0 " +
            "AND create_time >= #{start} AND create_time <= #{end} " +
            "GROUP BY pay_type")
    List<Map<String, Object>> revenueByPayType(@Param("start") String start,
                                               @Param("end") String end);

    /**
     * 每日出入场车次
     */
    @Select("SELECT DATE(in_time) AS `date`, COUNT(*) AS count " +
            "FROM parking_record " +
            "WHERE in_time >= #{start} AND in_time <= #{end} AND deleted = 0 " +
            "GROUP BY DATE(in_time) ORDER BY `date`")
    List<Map<String, Object>> inTrafficByDate(@Param("start") String start,
                                              @Param("end") String end);

    @Select("SELECT DATE(out_time) AS `date`, COUNT(*) AS count " +
            "FROM parking_record " +
            "WHERE out_time >= #{start} AND out_time <= #{end} AND deleted = 0 " +
            "GROUP BY DATE(out_time) ORDER BY `date`")
    List<Map<String, Object>> outTrafficByDate(@Param("start") String start,
                                               @Param("end") String end);
}
