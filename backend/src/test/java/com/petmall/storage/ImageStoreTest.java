package com.petmall.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 两种落盘方式的最小回归：本机写入真的落文件，远端配置错要在启动时报错、连不上要抛 IOException
 * 而不是让上传接口 500。
 */
class ImageStoreTest {

    @Test
    void localStoreWritesFileAndReturnsUploadUrl(@TempDir Path tempDir) throws IOException {
        LocalImageStore store = new LocalImageStore();
        ReflectionTestUtils.setField(store, "uploadPath", tempDir.toString());

        byte[] data = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
        String url = store.save(data, "png");

        assertTrue(url.matches("/upload/[0-9a-f\\-]{36}\\.png"), "URL 形态必须和历史数据一致：" + url);
        Path written = tempDir.resolve(url.substring("/upload/".length()));
        assertTrue(Files.exists(written), "文件应落在 uploadPath 下");
        assertArrayEquals(data, Files.readAllBytes(written));
    }

    @Test
    void sftpStoreFailsFastWhenHostMissing() {
        SftpImageStore store = new SftpImageStore();
        ReflectionTestUtils.setField(store, "host", "  ");
        ReflectionTestUtils.setField(store, "username", "petmall");
        ReflectionTestUtils.setField(store, "password", "x");
        ReflectionTestUtils.setField(store, "remoteDir", "/srv/petmall/upload");

        IllegalStateException e = assertThrows(IllegalStateException.class, store::validateConfig);
        assertTrue(e.getMessage().contains("FILE_SFTP_HOST"), e.getMessage());
    }

    @Test
    void sftpStoreFailsFastWhenNoCredential() {
        SftpImageStore store = new SftpImageStore();
        ReflectionTestUtils.setField(store, "host", "192.168.100.129");
        ReflectionTestUtils.setField(store, "username", "petmall");
        ReflectionTestUtils.setField(store, "password", "");
        ReflectionTestUtils.setField(store, "privateKey", "");
        ReflectionTestUtils.setField(store, "remoteDir", "/srv/petmall/upload");

        IllegalStateException e = assertThrows(IllegalStateException.class, store::validateConfig);
        assertTrue(e.getMessage().contains("private-key"), e.getMessage());
    }

    @Test
    void sftpStoreWrapsConnectFailureAsIOException() {
        SftpImageStore store = new SftpImageStore();
        ReflectionTestUtils.setField(store, "host", "127.0.0.1");
        ReflectionTestUtils.setField(store, "port", 1);   // 无人监听 → 连接立即被拒
        ReflectionTestUtils.setField(store, "username", "petmall");
        ReflectionTestUtils.setField(store, "password", "x");
        ReflectionTestUtils.setField(store, "privateKey", "");
        ReflectionTestUtils.setField(store, "remoteDir", "/srv/petmall/upload");
        ReflectionTestUtils.setField(store, "knownHosts", "");

        IOException e = assertThrows(IOException.class, () -> store.save(new byte[]{1, 2, 3}, "png"));
        assertTrue(e.getMessage().startsWith("连接文件服务器失败"), e.getMessage());
    }
}
