package com.delta.dms.community.utils;

import com.delta.set.utils.LogUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

import static com.delta.dms.community.utils.Constants.*;
import static com.delta.dms.community.utils.URLConstants.BACKEND_LANG_SEPARATOR;
import static com.delta.dms.community.utils.URLConstants.FRONTEND_LANG_SEPARATOR;

public class CustomLocaleResolver extends AcceptHeaderLocaleResolver {

    private static final LogUtil log = LogUtil.getInstance();

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        log.debug("CustomLocaleResolver.resolveLocale()");
        // 會先判斷query parameter，沒有才看request's accept-header
        // 前端lang會是用連字號相隔，所以要做轉換
        String langParam = request.getParameter(REQUEST_LOCALE_PARAM);
        if (langParam != null) {
            return new Locale(langParam.replace(FRONTEND_LANG_SEPARATOR, BACKEND_LANG_SEPARATOR));
        }
        String acceptLangHeader = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE).split(COMMA_DELIMITER)[0].trim();
        if (acceptLangHeader != null) {
            return new Locale(acceptLangHeader.replace(FRONTEND_LANG_SEPARATOR, BACKEND_LANG_SEPARATOR));
        }
        return new Locale(DEFAULT_I18N_LANG_CODE);
    }
}