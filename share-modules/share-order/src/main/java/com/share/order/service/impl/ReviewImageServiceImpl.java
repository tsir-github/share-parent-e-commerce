package com.share.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.domain.R;
import com.share.common.core.exception.ServiceException;
import com.share.order.domain.ReviewImage;
import com.share.order.mapper.ReviewImageMapper;
import com.share.order.service.IReviewImageService;
import com.share.system.api.RemoteFileService;
import com.share.system.api.domain.SysFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评价晒图 Service 实现
 *
 * @author share
 */
@Service
@RequiredArgsConstructor
public class ReviewImageServiceImpl extends ServiceImpl<ReviewImageMapper, ReviewImage> implements IReviewImageService {

    private final RemoteFileService remoteFileService;

    @Override
    public List<Map<String, Object>> uploadReviewImages(Long orderItemId, MultipartFile[] files, Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            R<SysFile> uploadResult = remoteFileService.upload(files[i]);
            if (uploadResult == null || uploadResult.getData() == null) {
                throw new ServiceException("图片上传失败: " + files[i].getOriginalFilename());
            }
            String url = uploadResult.getData().getUrl();

            ReviewImage ri = new ReviewImage();
            ri.setOrderItemId(orderItemId);
            ri.setImageUrl(url);
            ri.setSortOrder(i + 1);
            ri.setCreateBy(String.valueOf(userId));
            baseMapper.insert(ri);

            Map<String, Object> item = new HashMap<>();
            item.put("url", url);
            item.put("sortOrder", i + 1);
            result.add(item);
        }

        return result;
    }
}
