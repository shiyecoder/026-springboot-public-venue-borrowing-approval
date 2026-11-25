package com.grad.venue.common;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 全局异常处理：所有异常都以统一业务码返回，不把堆栈暴露给前端 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public R<Void> handleValid(Exception e) {
        String msg = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException m && m.getBindingResult().getFieldError() != null) {
            msg = m.getBindingResult().getFieldError().getDefaultMessage();
        } else if (e instanceof BindException b && b.getBindingResult().getFieldError() != null) {
            msg = b.getBindingResult().getFieldError().getDefaultMessage();
        }
        return R.fail(400, msg);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public R<Void> handleDenied(AccessDeniedException e) {
        return R.fail(403, "没有操作权限");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleOther(Exception e) {
        e.printStackTrace();
        return R.fail(500, "服务异常：" + e.getMessage());
    }
}
