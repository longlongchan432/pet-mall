package com.petmall.mapper;

import com.petmall.entity.RechargeRecord;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface RechargeRecordMapper {
    int insert(RechargeRecord record);
    List<RechargeRecord> selectByUserId(@Param("userId") Long userId);

    // 新增方法（匹配XML中的id）
    List<RechargeRecord> selectByPage(
            @Param("userId") Long userId,
            @Param("status") Integer status,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    Integer count(@Param("userId") Long userId, @Param("status") Integer status);

    RechargeRecord selectById(Long id);

    int updateAuditStatus(
            @Param("id") Long id,
            @Param("status") Integer status
    );
}
