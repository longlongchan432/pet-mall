package com.petmall.storage;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

/**
 * 经 SFTP 把图片写到文件服务器（Ubuntu），读取由那边的 nginx 直出，后端不背图片带宽。
 *
 * <p>ponytail: 每次上传新建一条 SSH 连接，无连接池。演示量级无感；高频上传时改连接池或对象存储 HTTP API。
 */
@Component
@ConditionalOnProperty(name = "file.storage.type", havingValue = "sftp", matchIfMissing = true)
public class SftpImageStore implements ImageStore {

    private static final Logger log = LoggerFactory.getLogger(SftpImageStore.class);

    private static final int CONNECT_TIMEOUT_MS = 8000;

    @Value("${file.sftp.host}")
    private String host;
    @Value("${file.sftp.port:22}")
    private int port;
    @Value("${file.sftp.username}")
    private String username;
    @Value("${file.sftp.password:}")
    private String password;
    @Value("${file.sftp.private-key:}")
    private String privateKey;
    @Value("${file.sftp.remote-dir}")
    private String remoteDir;
    @Value("${file.sftp.known-hosts:}")
    private String knownHosts;

    /** 配置错了要在启动时炸，而不是等到商户点上传才发现。 */
    @PostConstruct
    void validateConfig() {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalStateException("file.storage.type=sftp 但 file.sftp.host 为空，请设置 FILE_SFTP_HOST");
        }
        if ((password == null || password.isEmpty()) && (privateKey == null || privateKey.isEmpty())) {
            throw new IllegalStateException("file.sftp 需要 password 或 private-key 之一，请设置 FILE_SFTP_PASSWORD / FILE_SFTP_KEY");
        }
        if (knownHosts == null || knownHosts.isEmpty()) {
            log.warn("file.sftp.known-hosts 未配置，本次连接不做主机指纹校验（存在中间人风险）；生产环境请用 ssh-keyscan 生成 known_hosts 并填上");
        }
    }

    @Override
    public String save(byte[] data, String extension) throws IOException {
        String filename = UUID.randomUUID().toString() + "." + extension;
        String remotePath = remoteDir + "/" + filename;

        Session session = null;
        ChannelSftp channel = null;
        try {
            JSch jsch = new JSch();
            if (notEmpty(knownHosts)) {
                jsch.setKnownHosts(knownHosts);
            }
            if (notEmpty(privateKey)) {
                jsch.addIdentity(privateKey);
            }
            session = jsch.getSession(username, host, port);
            if (notEmpty(password)) {
                session.setPassword(password);
            }
            Properties config = new Properties();
            // 填了 known_hosts 就严格校验；没填只能关掉，并已在启动日志里点名风险
            config.put("StrictHostKeyChecking", notEmpty(knownHosts) ? "yes" : "no");
            if (notEmpty(knownHosts)) {
                config.put("UserKnownHostsFile", knownHosts);
            }
            session.setConfig(config);
            session.connect(CONNECT_TIMEOUT_MS);

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(CONNECT_TIMEOUT_MS);
            channel.put(new ByteArrayInputStream(data), remotePath);
            return "/upload/" + filename;
        } catch (JSchException | SftpException e) {
            throw new IOException("连接文件服务器失败（" + username + "@" + host + ":" + port + "）：" + e.getMessage(), e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
