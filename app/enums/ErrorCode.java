package enums;

/**
 * 错误码
 *
 * @author bowen
 */
public enum ErrorCode {

    _10001(10001, "用户名密码错误。");

    public int code;

    public String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}

