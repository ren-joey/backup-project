package com.delta.dms.community.utils;

public class URLConstants {
    public static final String COMMUNITY_URL_CONTEXT_ROOT = "%s/communityweb/%s/Community";
    public static final String COMMUNITY_TOPIC_URL = COMMUNITY_URL_CONTEXT_ROOT.
            concat("/Reply?topicId=%s");

    public static final String FRONTEND_LANG_SEPARATOR = "-"; // 前端lang都是en-us, zh-tw等等
    public static final String BACKEND_LANG_SEPARATOR = "_";
    public static final String URL_TRAILING_SLASH_REGEX = "/$";

    public static final String URL_ENCODED_SPACE = "%20";
    public static final String URL_ENCODED_SIDE_EFFECTS_SYMBOL = "+";

}
