package com.petmall.storage;

import java.io.IOException;

/**
 * 图片落盘。实现方决定落到本机还是远端文件服务器，调用方只认返回的对外 URL。
 * URL 契约固定为 {@code /upload/<uuid>.<ext>}，与数据库里已有的历史数据一致。
 */
public interface ImageStore {

    String save(byte[] data, String extension) throws IOException;
}
