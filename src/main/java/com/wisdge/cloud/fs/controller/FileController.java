package com.wisdge.cloud.fs.controller;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wisdge.cloud.dto.ApiResult;
import com.wisdge.cloud.dto.LoginUser;
import com.wisdge.cloud.fs.configurer.UploadLocker;
import com.wisdge.cloud.fs.internal.Constant;
import com.wisdge.cloud.fs.mapper.UploadHistoryMapper;
import com.wisdge.cloud.fs.po.UploadHistory;
import com.wisdge.commons.filestorage.FileStorage;
import com.wisdge.commons.filestorage.MultipartInputStreamSender;
import com.wisdge.dataservice.Result;
import com.wisdge.dataservice.utils.JSonUtils;
import com.wisdge.utils.DateUtils;
import com.wisdge.utils.FilenameUtils;
import com.wisdge.utils.ImageUtils;
import com.wisdge.utils.StringUtils;
import com.wisdge.web.springframework.WebUtils;
import com.wisdge.web.upload.CommonsMultipartResolverEx;
import com.wisdge.web.upload.WisdgeUploadController;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import sun.awt.image.ImageFormatException;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

@Slf4j
@Controller
@Api(tags="FileController")
public class FileController extends BaseController {
    private static final String FILEPATH_PATTERN_GUID = "{guid}";
    private static final String FILEPATH_PATTERN_DATE = "{date}";
    private static final String FILEPATH_PATTERN_DATE_EXP = "yyyyMMdd";
    private static final String FILEPATH_PATTERN_SOURCE = "{source}";
    private static final String FS_TEMP_FOLDER = "/temp";
    private static final String FS_ILLEGALFILEPATH = "fs.illegalFilepath";
    private static final String FS_FORBIDDEN = "fs.forbidden";
    private static final String FS_FILENOTEXIST = "fs.fileNotExist";
    private static final String FS_RETRIEVE_ERROR = "fs.retrieveError";
    private static final String FS_UPLOAD_LIMIT = "fs.uploadMaxCount";
    private static final String FS_IMAGE_ILLEGALLY = "fs.imageIllegally";
    private static final String FS_NOT_ALLOWED = "fs.notAllowed";
    private static final String FS_FORBIDDEN_SECURE = "fs.forbidden.secure";

    @Resource
    private FileStorage fileStorage;

    @Resource
    private UploadHistoryMapper uploadHistoryMapper;

    private long expireSeconds = 600;// 默认600秒超时
    private long maxLoadSize = 100 * 1024 * 1024;// 最大允许直接加载100M的文件

    private boolean isLocked() {
        UploadLocker uploadLocker = config.getUploadLocker();
        if (uploadLocker != null) {
            if (uploadLocker.getMinutes() < 1)
                return false;
            if (uploadLocker.getMaximal() < 1)
                return false;
        }

        LoginUser loginUser = getLoginUser();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, (-1) + uploadLocker.getMinutes());
        QueryWrapper<UploadHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("CREATE_BY", loginUser.getId())
                .eq("STATUS", 1)
                .gt("CREATE_TIME", calendar.getTime());

        int count = uploadHistoryMapper.selectCount(queryWrapper);
        log.debug("Detected {0} times upload in {1} minutes.", count, uploadLocker.getMinutes());
        if (count >= uploadLocker.getMaximal())
            return true;
        return false;
    }

    @PostMapping("/upload")
    @ResponseBody
    public ApiResult upload(HttpServletRequest request) throws IOException {
//        if (isLocked()) {
//            String exceptionMessage = i18n(FS_UPLOAD_LIMIT);
//            UploadHistory history = new UploadHistory();
//            history.setId(newId());
//            history.setStatus(-2);
//            history.setException(exceptionMessage);
//            history.setCreateBy(getLoginUser().getId());
//            history.setCreateTime(new Date());
//            uploadHistoryMapper.insert(history);
//            return ApiResult.fail(exceptionMessage);
//        }

        MultipartHttpServletRequest mr = CommonsMultipartResolverEx.getMultipartHttpServletRequest(request);
        String fsKey = WebUtils.getString(request, "key", Constant.FIELD_DEFAULT);
        String uploadPath = WebUtils.getString(request, "path", "");
        String saveName = WebUtils.getString(request, "name", "");
        boolean suffix = WebUtils.getBoolean(request, "suffix");
        boolean multipart = WebUtils.getBoolean(request, "multipart");

        /** 支持同时接收多个附件*/
        List<MultipartFile> mFiles = mr.getFiles("file");
        List<String> messages = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();
        for (MultipartFile mFile : mFiles) {
            InputStream inputStream = mFile.getInputStream();
            long size = mFile.getSize();
            String original = mFile.getOriginalFilename();
            String extension = FilenameUtils.getExtension(original).toLowerCase();
            // 文件上传白名单
            List<String> uploadAllow = config.getUploadAllow();
            if (uploadAllow != null) {
                boolean allowed = false;
                for (String allow : uploadAllow) {
                    if (extension.equalsIgnoreCase(allow)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed)
                    return ApiResult.fail(i18n(FS_NOT_ALLOWED, original));
            }
            // 文件上传黑名单
            List<String> uploadForbidden = config.getUploadForbidden();
            if (uploadForbidden != null) {
                for (String forbidden : uploadForbidden) {
                    if (extension.equalsIgnoreCase(forbidden)) {
                        return ApiResult.fail(i18n(FS_FORBIDDEN, original));
                    }
                }
            }

            if (!(StringUtils.isEmpty(saveName) || multipart)) {
                String simpleName = FilenameUtils.getBaseName(original);
                String saveExtension = FilenameUtils.getExtension(saveName).toLowerCase();
                if (uploadForbidden != null) {
                    for (String forbidden : uploadForbidden) {
                        // 禁止上传特定的文件
                        if (saveExtension.equalsIgnoreCase(forbidden)) {
                            return ApiResult.fail(i18n(FS_FORBIDDEN, saveName));
                        }
                    }
                }
                saveName = fileStorage.filterFilename(saveName)
                        .replace(FILEPATH_PATTERN_GUID, UUID.randomUUID().toString())
                        .replace(FILEPATH_PATTERN_DATE, DateUtils.format(new Date(), FILEPATH_PATTERN_DATE_EXP))
                        .replace(FILEPATH_PATTERN_SOURCE, fileStorage.filterFilename(simpleName));
            } else {
                saveName = "";
            }
            String filename = (StringUtils.isEmpty(saveName) ? FilenameUtils.getBaseName(original) : saveName);
            if (suffix)
                filename += "_" + new Date().getTime();
            filename += StringUtils.isEmpty(extension) ? "" : ("." + extension);
            if (StringUtils.isEmpty(uploadPath)) {
                uploadPath = FS_TEMP_FOLDER;
            } else {
                uploadPath = fileStorage.filterFilepath(uploadPath).replace(FILEPATH_PATTERN_DATE, DateUtils.format(new Date(), FILEPATH_PATTERN_DATE_EXP)).trim();
            }
            Result result = fileStorage.saveStream(fsKey, uploadPath, original, filename, inputStream, size, (current,total)->{});
            UploadHistory history = new UploadHistory();
            history.setId(newId());
            LoginUser loginUser = getLoginUser();
            history.setCreateBy(loginUser == null ? null : getLoginUser().getId());
            history.setCreateTime(new Date());
            if (result.getCode() < 0) {
                history.setStatus(-1);
                history.setException(result.getMessage());
            } else {
                history.setStatus(1);
                history.setFileName(original);
                history.setStoragePath((String) result.getValue());
                history.setFileSize(size*1d / 1000);
            }
            uploadHistoryMapper.insert(history);
            if (! multipart) {
                if (result.getCode() <= 0){
                    return ApiResult.fail(result.getMessage());
                } else{
                    return ApiResult.ok(original, result.getValue());
                }
            }
            messages.add(result.getMessage());
            values.add(result.getValue());
            if (inputStream != null){
                inputStream.close();
            }
        }
        return ApiResult.ok(new JSONArray(Collections.singletonList(messages)).toString(), values);
    }

    @GetMapping("/upload/status")
    @ResponseBody
    public ApiResult uploadStatus() {
        WisdgeUploadController wuc = new WisdgeUploadController();
        Result result = wuc.uploadStatusQuery(request);
        if (result.getCode() > 0)
            return ApiResult.ok(result.getMessage(), result.getValue());
        else
            return ApiResult.fail(result.getMessage());
    }

    @PostMapping("/cutImage")
    @ResponseBody
    public ApiResult cutImage(HttpServletRequest request) throws Exception {
        String fsKey = WebUtils.getString(request, "key", Constant.FIELD_DEFAULT);
        String uploadPath = WebUtils.getString(request, "path", "");
        String saveName = WebUtils.getString(request, "name", "");
        boolean suffix = WebUtils.getBoolean(request, "suffix");
        String original = WebUtils.getString(request, "original");
        String coords = WebUtils.getString(request, "coords", "");
        log.debug("CutImage: {}", coords);

        Map<String, Integer> coordsMap = (Map<String, Integer>) JSonUtils.read(coords, Map.class);
        int x = coordsMap.get("x");
        int y = coordsMap.get("y");
        int width = coordsMap.get("w");
        int height = coordsMap.get("h");
        int imgWidth = coordsMap.get("iw");
        int cropWidth = coordsMap.get("cw");
        int cropHeight = coordsMap.get("ch");

        byte[] fileData;
        if (StringUtils.isEmpty(original)) {
            // multipart-data
            MultipartHttpServletRequest mr = CommonsMultipartResolverEx.getMultipartHttpServletRequest(request);
            MultipartFile mFile = mr.getFile("file");
            fileData = mFile.getBytes();
            original = mFile.getOriginalFilename();
        } else {
            String oldFsKey = Constant.FIELD_DEFAULT;
            int idx = original.indexOf('@');
            if (idx != -1) {
                oldFsKey = original.substring(0, idx);
                original = original.substring(idx + 1);
            }
            fileData = fileStorage.retrieveFile(oldFsKey, original);
            if (fileData == null)
                return ApiResult.fail(i18n(FS_RETRIEVE_ERROR));
        }
        String extension = FilenameUtils.getExtension(original).toLowerCase();
        if (StringUtils.isNotEmpty(saveName)) {
            String simpleName = FilenameUtils.getBaseName(original);
            saveName = fileStorage.filterFilename(saveName)
                    .replace(FILEPATH_PATTERN_GUID, UUID.randomUUID().toString())
                    .replace(FILEPATH_PATTERN_DATE, DateUtils.format(new Date(), FILEPATH_PATTERN_DATE_EXP))
                    .replace(FILEPATH_PATTERN_SOURCE, fileStorage.filterFilename(simpleName));
        } else {
            saveName = "";
        }

        String filename = (StringUtils.isEmpty(saveName) ? FilenameUtils.getBaseName(original) : saveName);
        if (suffix)
            filename += "_" + new Date().getTime();
        filename += StringUtils.isEmpty(extension) ? "" : ("." + extension);

        if (StringUtils.isEmpty(uploadPath)) {
            uploadPath = FS_TEMP_FOLDER;
        } else {
            uploadPath = fileStorage.filterFilepath(uploadPath).replace(FILEPATH_PATTERN_DATE, DateUtils.format(new Date(), FILEPATH_PATTERN_DATE_EXP)).trim();
        }

        BufferedImage bufferedImage = ImageUtils.getImageFromBytes(fileData);
        if (bufferedImage == null)
            throw new ImageFormatException(i18n(FS_IMAGE_ILLEGALLY, original));
        float scale = Math.min(10, bufferedImage.getWidth() * 1f / imgWidth);
        x = (int) (x * scale);
        y = (int) (y * scale);
        width = Math.min(bufferedImage.getWidth() - x, (int) (width * scale));
        height = Math.min(bufferedImage.getHeight() - y, (int) (height * scale));
        Thumbnails.Builder<BufferedImage> builder = Thumbnails.of(bufferedImage).sourceRegion(x, y, width, height);
        if (cropWidth > 0 || cropHeight > 0) {
            cropWidth = (int) (cropWidth * scale);
            cropHeight = (int) (cropHeight * scale);
            if (cropWidth == 0)
                cropWidth = width;
            if (cropHeight == 0)
                cropHeight = height;
            log.debug("Crop image as new size: {} x {}", cropWidth, cropHeight);
            builder = builder.size(cropWidth, cropHeight).keepAspectRatio(true);
        } else
            builder = builder.scale(1);
        BufferedImage newImage = resizeImage(builder.outputQuality(1.0f).asBufferedImage(), 1960, 1080);
        fileData = ImageUtils.getBytesFromImage(newImage, ImageUtils.getImageType(original));
        Result result = fileStorage.saveFile(fsKey, uploadPath, original, filename, fileData);
        if (result.getCode() > 0)
            return ApiResult.ok(result.getMessage(), result.getValue());
        else
            return ApiResult.fail(result.getMessage());
    }

    private BufferedImage resizeImage(BufferedImage image, int width, int height) throws IOException {
        if (image.getWidth() <= width && image.getHeight() <= height)
            return image;
        return Thumbnails.of(image).size(width, height).keepAspectRatio(true).asBufferedImage();
    }

    @GetMapping("/get")
    public ModelAndView get() {
        Boolean download = WebUtils.getBoolean(request, "d");
        String filepath = WebUtils.getString(request, "file", "");
        String asName = WebUtils.getString(request, "as", "");
        if (StringUtils.isEmpty(filepath) || "undefined".equals(filepath)) {
            return error(i18n(FS_ILLEGALFILEPATH), HttpStatus.NOT_ACCEPTABLE);
        }

        String fsKey = Constant.FIELD_DEFAULT;
        int idx = filepath.indexOf('@');
        if (idx != -1) {
            fsKey = filepath.substring(0, idx);
            filepath = filepath.substring(idx + 1);
        }
        if (fileStorage.isSecurity(fsKey)) {
            return error(i18n(FS_FORBIDDEN_SECURE), HttpStatus.FORBIDDEN);
        }

        try {
            allowCrossDomain();
            // 参数c=true表示考虑缓存，如果检测出可以读取缓存，则不读取文件直接返回301，客户端读取缓存信息
            if (WebUtils.getBoolean(request, "c") && !checkHeaderCache(expireSeconds, 0)) {
                return null;
            }
            String filename = StringUtils.isEmpty(asName) ? FilenameUtils.getName(filepath) : asName;
//			String contentType = download ? "application/octet-stream" : WebUtils.getContentType(FilenameUtils.getExtension(filename));
            fileStorage.retrieveStream(fsKey, filepath, (is, metadata) -> {
                if (StringUtils.isNotEmpty(metadata.getDownloadURL())) {
                    try {
                        metadata.setDownloadURL(metadata.getDownloadURL().replace("http","https"));
                        response.sendRedirect(metadata.getDownloadURL());// 直接通过文件下载地址重定向下载
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                    return;
                }
                try {
                    if (isNeedDetect(FilenameUtils.getExtension(filename)) // 需要检测charset的
                            && metadata.getContentLength() < maxLoadSize) { // 并且文件大小小于最大允许的加载大小时候（避免OOM）
                        byte[] data = IOUtils.toByteArray(is);
                        out(data, filename, download);
                    } else {
                        // 不需要检测的，按流支持分段输出
                        metadata.setFileName(filename);
//						metadata.setContentType(contentType);
                        MultipartInputStreamSender.from(is, metadata)
                                .with(request)
                                .with(response)
                                .serveResource();
                        if (is != null){
                            is.close();
                        }
                    }
                } catch (Exception e) {
                    log.error("IO Error", e);
                }
            });
            return null;
        } catch (FileNotFoundException e) {
            return error(i18n(FS_FILENOTEXIST, filepath), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Retrieve file error", e);
            return error(i18n(FS_RETRIEVE_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/thumb")
    public ModelAndView thumb() {
        String filepath = WebUtils.getString(request, "file", "");
        if (StringUtils.isEmpty(filepath)) {
            return error(i18n(FS_ILLEGALFILEPATH), HttpStatus.NOT_ACCEPTABLE);
        }

        String fsKey = Constant.FIELD_DEFAULT;
        int idx = filepath.indexOf('@');
        if (idx != -1) {
            fsKey = filepath.substring(0, idx);
            filepath = filepath.substring(idx + 1);
        }
        if (fileStorage.isSecurity(fsKey)) {
            return error(i18n(FS_FORBIDDEN), HttpStatus.FORBIDDEN);
        }

        try {
            allowCrossDomain();
            // 参数c=true表示考虑缓存，如果检测出可以读取缓存，则不读取文件直接返回301，客户端读取缓存信息
            if (WebUtils.getBoolean(request, "c") && !checkHeaderCache(expireSeconds, 0)) {
                return null;
            }
            byte[] data = fileStorage.retrieveFile(fsKey, filepath);
            // 缩率图
            int width = WebUtils.getInteger(request, "w", 120);//图像宽度，默认120px
            int height = WebUtils.getInteger(request, "h", 120);//图像高度，默认120px
            double quality = WebUtils.getDouble(request, "q", 1.0);//图像质量，0-1之间一个浮点数，1表示原图质量
            int oldsize = data.length;
            try (
                    InputStream is = new ByteArrayInputStream(data);
                    ByteArrayOutputStream os = new ByteArrayOutputStream()
            ) {
                Thumbnails.of(is)
                        .size(Math.min(width, 1920), Math.min(height, 1080))
                        .outputQuality(Math.min(quality, 1))
                        .toOutputStream(os);
                data = os.toByteArray();
                log.debug("{}x{} - {}, Original size: {}, Thumb size:{}", width, height, quality, oldsize, data.length);
                out(data, FilenameUtils.getName(filepath), false);
            }
            return null;
        } catch (FileNotFoundException e) {
            return error(i18n(FS_FILENOTEXIST, filepath), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Retrieve file error", e);
            return error(i18n(FS_RETRIEVE_ERROR), HttpStatus.BAD_REQUEST);
        }
    }

    private ModelAndView error(String message, HttpStatus status) {
        log.debug("Status:{}, Message:{}", status, message);
        ModelAndView model = new ModelAndView("/error", status);
        model.addObject("error", message);
        return model;
    }

    @PostMapping("/ckeditor/upload")
    @ResponseBody
    public ApiResult ckeditorUpload(HttpServletRequest request) {
        MultipartHttpServletRequest mr = CommonsMultipartResolverEx.getMultipartHttpServletRequest(request);
        String fsKey = WebUtils.getString(request, "key", Constant.FIELD_DEFAULT);
        String uploadPath = WebUtils.getString(request, "path", "/ckeditor/{date}");
        String saveName = WebUtils.getString(request, "name");
        boolean suffix = true;
        try {
            /** 仅接收单个附件 **/
            MultipartFile mFile = mr.getFile("file");
            InputStream inputStream = mFile.getInputStream();
            long size = mFile.getSize();
            String original = mFile.getOriginalFilename();
            String extension = FilenameUtils.getExtension(original).toLowerCase();
            // 文件上传白名单
            List<String> uploadAllow = config.getUploadAllow();
            if (uploadAllow != null) {
                boolean allowed = false;
                for (String allow : uploadAllow) {
                    if (extension.equalsIgnoreCase(allow)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) {
                    return ApiResult.fail(i18n(FS_NOT_ALLOWED, original));
                }
            }
            // 文件上传黑名单
            List<String> uploadForbidden = config.getUploadForbidden();
            if (uploadForbidden != null) {
                for (String forbidden : uploadForbidden) {
                    if (extension.equalsIgnoreCase(forbidden)) {
                        return ApiResult.fail(i18n(FS_FORBIDDEN, original));
                    }
                }
            }

            if (StringUtils.isNotEmpty(saveName)) {
                String simpleName = FilenameUtils.getBaseName(original);
                String saveExtension = FilenameUtils.getExtension(saveName).toLowerCase();
                if (uploadForbidden != null) {
                    for (String forbidden : uploadForbidden) {
                        // 禁止上传特定的文件
                        if (saveExtension.equalsIgnoreCase(forbidden)) {
                            return ApiResult.fail(i18n(FS_FORBIDDEN, saveName));
                        }
                    }
                }
                saveName = fileStorage.filterFilename(saveName)
                        .replace(FILEPATH_PATTERN_GUID, UUID.randomUUID().toString())
                        .replace(FILEPATH_PATTERN_DATE, DateUtils.format(new Date(), FILEPATH_PATTERN_DATE_EXP))
                        .replace(FILEPATH_PATTERN_SOURCE, fileStorage.filterFilename(simpleName));
            } else {
                saveName = "";
            }
            String filename = (StringUtils.isEmpty(saveName) ? FilenameUtils.getBaseName(original) : saveName);
            if (suffix)
                filename += "_" + new Date().getTime();
            filename += StringUtils.isEmpty(extension) ? "" : ("." + extension);
            uploadPath = fileStorage.filterFilepath(uploadPath).replace(FILEPATH_PATTERN_DATE, DateUtils.format(new Date(), FILEPATH_PATTERN_DATE_EXP)).trim();
            Result result = fileStorage.saveStream(fsKey, uploadPath, original, filename, inputStream, size,(current,total)->{});
            if (result.getCode() < 0)
                return ApiResult.internalError(result.getMessage());
            return ApiResult.ok(original, result.getValue());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ApiResult.internalError(e.getMessage());
        }
    }
}
