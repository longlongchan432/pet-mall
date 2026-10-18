package com.petmall.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/** 存本机磁盘：无虚拟机也能起后端，同时是远端方案的回滚路径。 */
@Component
@ConditionalOnProperty(name = "file.storage.type", havingValue = "local")
public class LocalImageStore implements ImageStore {

    @Value("${file.upload.path}")
    private String uploadPath;

    @Override
    public String save(byte[] data, String extension) throws IOException {
        // 相对路径必须转绝对：按 JVM 工作目录解析会和 WebConfig 的静态映射指向不同地方
        Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        Files.createDirectories(baseDir);
        String filename = UUID.randomUUID().toString() + "." + extension;
        Files.write(baseDir.resolve(filename), data);
        return "/upload/" + filename;
    }
}
