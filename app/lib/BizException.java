package lib;

import enums.ErrorCode;

/**
 * @author bowen
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 6422357307660524879L;

    public int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.msg);
        this.code = errorCode.code;
    }
}
