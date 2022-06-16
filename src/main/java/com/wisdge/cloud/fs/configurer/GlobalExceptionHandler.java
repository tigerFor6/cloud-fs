package com.wisdge.cloud.fs.configurer;

import com.wisdge.cloud.component.MessagesLocaleResolver;
import com.wisdge.cloud.dto.ApiResult;
import com.wisdge.cloud.dto.ResultCode;
import com.wisdge.cloud.exception.IllegallySignatureException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.AuthenticationException;
import org.apache.ibatis.builder.BuilderException;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import javax.annotation.Resource;
import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Resource
    private MessagesLocaleResolver localeResolver;

    /**
     * 参数未通过验证异常
     * @param exception MethodArgumentNotValidException
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult MethodArgumentNotValidExceptionHandler(MethodArgumentNotValidException exception) {
        return ApiResult.build(ResultCode.BAD_REQUEST.getCode(), exception.getBindingResult().getFieldError().getDefaultMessage());
    }

    /**
     * 无法解析参数异常
     * @param exception HttpMessageNotReadableException
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult HttpMessageNotReadableExceptionHandler(HttpMessageNotReadableException exception) throws Exception {
        return ApiResult.fail("参数无法正常解析");
    }

    /**
     * 方法访问权限不足异常
     * @param exception AccessDeniedException
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ApiResult AccessDeniedExceptionHandler(AccessDeniedException exception) throws Exception {
        return ApiResult.forbidden("方法访问权限不足");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ApiResult NoHandlerFoundExceptionHandler(NoHandlerFoundException exception) throws Exception {
        return ApiResult.notFound("链接不存在");
    }

    @ExceptionHandler(IllegallySignatureException.class)
    public ApiResult IllegallySignatureExceptionHandler(IllegallySignatureException exception) throws Exception {
        return ApiResult.fail("签名错误");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ApiResult AuthenticationExceptionHandler(AuthenticationException e) {
        return ApiResult.forbidden();
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ApiResult DuplicateKeyExceptionHandler(DuplicateKeyException e) throws Exception {
        String source = e.getMessage();
        Pattern pattern = Pattern.compile("Duplicate entry '(.*)' for key '(.*)'");
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            String field = matcher.group(2);
            return ApiResult.internalError("唯一性约束" + field + "检查失败");
        } else
            return ApiResult.internalError("数据库字段唯一性检查失败");
    }

    @ExceptionHandler(BuilderException.class)
    public ApiResult BuilderExceptionHandler(BuilderException e) {
        return ApiResult.internalError("数据库语法错误");
    }

    @ExceptionHandler(value = MyBatisSystemException.class)
    public ApiResult MyBatisSystemExceptionHandler(MyBatisSystemException e) {
        return ApiResult.internalError("数据库语法错误");
    }

    @ExceptionHandler(DataAccessException.class)
    public ApiResult DataAccessExceptionHandler(DataAccessException e) {
        return ApiResult.internalError("数据库访问错误");
    }

    @ExceptionHandler(SQLException.class)
    public ApiResult SQLExceptionHandler(SQLException e) {
        return ApiResult.internalError("数据库访问错误");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResult MaxUploadSizeExceededExceptionHandler(MaxUploadSizeExceededException e) {
        return ApiResult.fail(i18n("maxUploadSize"));
    }

    @ExceptionHandler(MultipartException.class)
    public ApiResult MultipartExceptionHandle(Throwable t) {
        return ApiResult.fail("文件上传失败");
    }

    @ExceptionHandler(value = Exception.class)
    public ApiResult exceptionHandler(Exception e) {
        log.error(e.getMessage(), e);
        return ApiResult.internalError("服务器请求异常");
    }

    private String i18n(String key, Object... objects) {
        ResourceBundle bundle = ResourceBundle.getBundle("resources/exception", localeResolver.getDefaultLocale());
        if (bundle.containsKey(key)) {
            String resource = bundle.getString(key);
            return objects != null && objects.length > 0 ? MessageFormat.format(resource, objects) : resource;
        } else {
            return key;
        }
    }
}
