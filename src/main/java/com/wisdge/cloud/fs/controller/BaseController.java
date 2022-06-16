package com.wisdge.cloud.fs.controller;

import com.wisdge.cloud.fs.configurer.Config;
import com.wisdge.cloud.dto.LoginUser;
import com.wisdge.cloud.internal.CoreConstant;
import com.wisdge.commons.redis.RedisTemplate;
import com.wisdge.utils.FileUtils;
import com.wisdge.utils.FilenameUtils;
import com.wisdge.utils.SnowflakeIdWorker;
import com.wisdge.web.springframework.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
public abstract class BaseController extends com.wisdge.cloud.controller.BaseController {
    @Autowired
    protected Config config;

    @Autowired
    protected SnowflakeIdWorker snowflakeIdWorker;

    protected LoginUser getLoginUser() {
        return (LoginUser) request.getAttribute(CoreConstant.JWT_REQUEST_USER);
    }

    protected String newId() {
        return String.valueOf(snowflakeIdWorker.nextId());
    }

    @Resource
    protected RedisTemplate redisTemplate;

    protected void out(String content) {
        out(content.getBytes());
    }

    protected void out(byte[] content) {
        out(content, StandardCharsets.UTF_8, "text/plain");
    }

    protected void out(byte[] content, Charset charset) {
        out(content, charset, "text/plain");
    }

    protected void out(byte[] content, Charset charset, String contentType) {
        out(content, charset, contentType, null);
    }

    protected void out(byte[] content, Charset charset, String contentType, String filename) {
        try {
            response.reset();
            response.addHeader("Content-Length", "" + content.length);
//			response.setHeader("Pragma", "no-cache");
//			response.addHeader("Cache-Control", "no-cache");
//			response.setDateHeader("Expires", 0);
            response.setContentType(contentType + ";charset=" + charset);
            if (filename != null) {
                // Fixed by Kevin
                // 前台提交的中文文件名，如果不使用编码，则会出现 ___.xx的文件名异常
                response.addHeader("Content-Disposition", "attachment;filename=\"" + buildEncode(filename) + "\"");
                response.addHeader("Content-Length", "" + content.length);
            }
            ServletOutputStream out = response.getOutputStream();
            try {
                out.write(content);
                out.flush();
            } finally {
                out.close();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    protected void out(byte[] data, String filename, boolean download) throws Exception {
        if (isNeedDetect(FilenameUtils.getExtension(filename))) {
            Charset charset = FileUtils.detect(new ByteArrayInputStream(data), data.length);
            if (charset == null)
                throw new UnsupportedEncodingException("无法获取文件字符集");
            response.setCharacterEncoding(charset.displayName());
        }
        if (data == null || data.length == 0) {
            log.error("文件不存在：{}", filename);
            response.sendError(404, "文件不存在：" + filename);
        } else {
            try {
                WebUtils.out(request, response, data, filename, download);
            } catch (Exception e) {
                log.error("Output stream error", e);
            }
        }
    }

    protected boolean isNeedDetect(String fileExtention) {
        fileExtention = fileExtention.toLowerCase();
        String[] extensions = new String[]{
                "txt", "text", "html", "xml", "htm", "js", "css"
        };
        for (String extension : extensions) {
            if (extension.equalsIgnoreCase(fileExtention))
                return true;
        }
        return false;
    }

    protected String buildEncode(String filename) {
        String encode = config.getOutEncode();
        try {
            if (encode.equalsIgnoreCase(StandardCharsets.UTF_8.displayName())) {
                return URLEncoder.encode(filename, StandardCharsets.UTF_8.displayName()).replaceAll("%28", "(").replaceAll("%29", ")");
            } else {
                return new String(filename.getBytes(StandardCharsets.UTF_8), encode);
            }
        } catch (UnsupportedEncodingException e) {
            log.error(e.getLocalizedMessage(), e);
            return filename;
        }
    }

    protected void allowCrossDomain() {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "*");
    }

    /**
     * 检查是否可以使用缓存，返回true表示需要重新读取，返回false表示不需要重新读取可以直接用缓存
     * If-Modified-Since对应于Last-Modified
     * If-None-Match对应于Etag
     *
     * @param expireSeconds         多少秒过期
     * @param modelLastModifiedDate
     * @return
     */
    protected boolean checkHeaderCache(long expireSeconds, long modelLastModifiedDate) {
        long expire = expireSeconds * 1000;
        long header = request.getDateHeader("If-Modified-Since");
        long now = System.currentTimeMillis();
        if (header > 0 && expire > 0) {
            if (modelLastModifiedDate > header) {
                // adddays = 0; // reset
                response.setStatus(HttpServletResponse.SC_OK);
                return true;
            }
            if (header + expire > now) {
                // during the period happend modified
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return false;
            }
        }

        // if over expire data, see the Etags;
        // ETags if ETags no any modified
        String previousToken = request.getHeader("If-None-Match");
        if (modelLastModifiedDate != 0 && previousToken != null && previousToken.equals(Long.toString(modelLastModifiedDate))) {
            // not modified
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return false;
        }
        response.setHeader("ETag", Long.toString(modelLastModifiedDate));
        setRespHeaderCache(expireSeconds);
        return true;
    }

    /**
     * 设置缓存相关的响应头
     *
     * @param expireSeconds 超时秒数
     * @return
     */
    protected boolean setRespHeaderCache(long expireSeconds) {
        long expires = expireSeconds * 1000;
        String maxAgeDirective = "max-age=" + expires;
        response.setHeader("Cache-Control", maxAgeDirective);
        response.setStatus(HttpServletResponse.SC_OK);
        response.addDateHeader("Last-Modified", System.currentTimeMillis());
        response.addDateHeader("Expires", System.currentTimeMillis() + expires);
        return true;
    }


}
