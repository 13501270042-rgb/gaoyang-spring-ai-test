package com.kakuiwong.gaoyangspringai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakuiwong.gaoyangspringai.entity.RequestTask;
import org.apache.ibatis.annotations.Param;

/**
 * @author: gaoyang
 * @Description:
 */
public interface RequestTaskMapper extends BaseMapper<RequestTask> {

    Long semaphore(@Param("userId") Integer  userId, @Param("maxQueueSize")  int maxQueueSize,
                  @Param("queueWaitSeconds")  long queueWaitSeconds);
}
