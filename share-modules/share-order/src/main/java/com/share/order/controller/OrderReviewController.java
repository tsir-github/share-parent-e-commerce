package com.share.order.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.RequiresLogin;
import com.share.common.security.utils.SecurityUtils;
import com.share.order.service.IReviewImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * C端评价晒图
 *
 * @author share
 */
@Tag(name = "评价晒图")
@RestController
@RequestMapping("/api/v1/order/review")
@RequiredArgsConstructor
public class OrderReviewController {

    private final IReviewImageService reviewImageService;

    @Operation(summary = "上传评价晒图")
    @RequiresLogin
    @PostMapping(value = "/{orderItemId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<List<Map<String, Object>>> uploadReviewImages(
            @PathVariable Long orderItemId,
            @RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return R.fail("文件不能为空");
        }
        if (files.length > 9) {
            return R.fail("单次最多上传9张图片");
        }
        for (MultipartFile f : files) {
            String ct = f.getContentType();
            if (ct == null || !ct.startsWith("image/")) {
                return R.fail("只允许上传图片文件: " + f.getOriginalFilename());
            }
            if (f.getSize() > 10 * 1024 * 1024) {
                return R.fail("图片大小不能超过10MB: " + f.getOriginalFilename());
            }
        }
        return R.ok(reviewImageService.uploadReviewImages(orderItemId, files, SecurityUtils.getUserId()));
    }
}
