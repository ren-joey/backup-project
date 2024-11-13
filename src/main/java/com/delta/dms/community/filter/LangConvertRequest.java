package com.delta.dms.community.filter;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Arrays;

import static com.delta.dms.community.utils.Constants.REQUEST_LOCALE_PARAM;
import static com.delta.dms.community.utils.URLConstants.BACKEND_LANG_SEPARATOR;
import static com.delta.dms.community.utils.URLConstants.FRONTEND_LANG_SEPARATOR;

public class LangConvertRequest extends HttpServletRequestWrapper {
    public LangConvertRequest(ServletRequest request) {
        super((HttpServletRequest) request);
    }

    @Override
    public String getParameter(String paramName) {
        if (paramName.equals(REQUEST_LOCALE_PARAM)){
            String param = super.getParameter(paramName);
            return param == null? null : super.getParameter(paramName)
                    .replace(FRONTEND_LANG_SEPARATOR,  BACKEND_LANG_SEPARATOR);
        }
        return super.getParameter(paramName);
    }

    @Override
    public String[] getParameterValues(String paramName) {
        if (paramName.equals(REQUEST_LOCALE_PARAM)) {
            return Arrays.stream(super.getParameterValues(paramName))
                    .map(p -> p.replace(FRONTEND_LANG_SEPARATOR,  BACKEND_LANG_SEPARATOR))
                    .toArray(String[]::new);
        }
        return super.getParameterValues(paramName);
    }
}
