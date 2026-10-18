package com.petmall.controller;

import com.petmall.common.Result;
import com.petmall.storage.ImageStore;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/file")
public class FileController {

    private final ImageStore imageStore;

    public FileController(ImageStore imageStore) {
        this.imageStore = imageStore;
    }

    // 上传目录只放图片，不能收 html/js/svg 这类可在浏览器里执行的内容
    private static final Set<String> ALLOWED_EXT =
            new HashSet<>(Arrays.asList("png", "jpg", "jpeg", "gif", "webp", "bmp"));

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        int dot = originalFilename == null ? -1 : originalFilename.lastIndexOf(".");
        String extension = dot >= 0 ? originalFilename.substring(dot + 1).toLowerCase() : "";
        if (!ALLOWED_EXT.contains(extension)) {
            return Result.error("只允许上传图片（png/jpg/jpeg/gif/webp/bmp）");
        }
        try {
            // 后缀可以随手改，再验一次文件头，挡住改名伪装的文件；先验后落盘，远端也不收脏文件
            byte[] head = new byte[12];
            int read;
            try (InputStream in = file.getInputStream()) {
                read = in.read(head);
            }
            if (!looksLikeImage(head, read)) {
                return Result.error("文件内容不是有效图片");
            }

            String url = imageStore.save(file.getBytes(), extension);

            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            return Result.success(data);
        } catch (IOException e) {
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

    /** 超过 multipart 上限时给一句人话，不要 500 堆栈。 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> tooLarge(MaxUploadSizeExceededException e) {
        return Result.error("图片过大，单张不要超过 5MB");
    }

    /** 按文件头签名判断是不是图片，不信任后缀和 Content-Type。 */
    private static boolean looksLikeImage(byte[] h, int n) {
        if (n >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return true;       // PNG
        if (n >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return true; // JPEG
        if (n >= 6 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F') return true;                                // GIF
        if (n >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return true;                  // WEBP
        if (n >= 2 && h[0] == 'B' && h[1] == 'M') return true;                                              // BMP
        return false;
    }
}
