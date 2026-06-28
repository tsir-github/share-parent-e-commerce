package com.share.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.order.domain.ReviewImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 评价晒图 Service 接口
 *
 * @author share
 */
public interface IReviewImageService extends IService<ReviewImage> {

    /**
     * 上传评价晒图
     *
     * @param orderItemId 订单项ID
     * @param files       图片文件数组
     * @param userId      用户ID
     * @return 图片URL列表（含sortOrder）
     */
    List<Map<String, Object>> uploadReviewImages(Long orderItemId, MultipartFile[] files, Long userId);
}
