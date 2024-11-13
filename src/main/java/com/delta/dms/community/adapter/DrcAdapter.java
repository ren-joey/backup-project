/***********************************************************
 *  Created: 2024/03
 *  Author: MARK.TSAO
 *  Goal: 透過此adapter將topic的文字與Id組成檔案打drcSync的API
 */
package com.delta.dms.community.adapter;

import com.delta.dms.community.config.DrcSyncConfig;
import com.delta.dms.community.dao.DrcSyncDao;
import com.delta.dms.community.model.DrcSyncSignIn;
import com.delta.dms.community.model.DrcSyncTokens;
import com.delta.dms.community.model.DrcSyncUser;
import com.delta.set.utils.LogUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import java.time.Instant;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import static com.delta.dms.community.utils.Constants.*;

@Component
public class DrcAdapter {
    private final AdapterUtil adapterUtil;
    private final DrcSyncDao drcSyncDao;
    private final DrcSyncConfig drcSyncConfig;
    private static final LogUtil log = LogUtil.getInstance();
    private static final String SIGNIN_URL = "user/signin";
    private static final String UPSERT_URL = "file/upsert";
    private static final String DELETE_URL = "file/delete";

    @Autowired
    public DrcAdapter(AdapterUtil adapterUtil,
                      DrcSyncDao drcSyncDao,
                      DrcSyncConfig drcSyncConfig) {
        this.adapterUtil = adapterUtil;
        this.drcSyncDao = drcSyncDao;
        this.drcSyncConfig = drcSyncConfig;
    }

    /**
     * 處理 request 打API，
     * 並且記錄 API response 結果到DB
     *
     * @return API Response status code, and body
     */
    private AbstractMap.SimpleEntry<Integer, String> handleRequest(
            String url,
            HttpMethod method,
            HttpHeaders headers,
            MultiValueMap<String, Object> requestBody,
            MultiValueMap<String, String> urlParams,
            String action,
            int topicId,
            int communityId,
            int forumId) {

        try {
            ResponseEntity<String> response = adapterUtil.sendRequestWithCustomHeader(
                    url, method, requestBody, urlParams, headers, String.class);
            drcSyncDao.updateFileStatus(
                    drcSyncConfig.getDatabase(),
                    action,
                    topicId,
                    communityId,
                    forumId,
                    response.getStatusCodeValue(),
                    response.getBody());
            return new AbstractMap.SimpleEntry<>(response.getStatusCodeValue(), response.getBody());
        } catch (RestClientResponseException e) {
            log.debug("HTTP status: " + e.getRawStatusCode() + ", Response body: " + e.getResponseBodyAsString());
            drcSyncDao.updateFileStatus(
                    drcSyncConfig.getDatabase(),
                    action,
                    topicId,
                    communityId,
                    forumId,
                    e.getRawStatusCode(),
                    e.getResponseBodyAsString());
            return new AbstractMap.SimpleEntry<>(e.getRawStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("An error occurred while sending file to DRC Sync: " + e.getMessage());
            drcSyncDao.updateFileStatus(
                    drcSyncConfig.getDatabase(),
                    action,
                    topicId,
                    communityId,
                    forumId,
                    -1,
                    e.getMessage());
            return new AbstractMap.SimpleEntry<>(-1, e.getMessage());
        }
    }

    /**
     * 將topic的文字與Id組成檔案當作request body，
     * 透過 handleRequest 拿到打API的結果，
     * create 與 update 的 request body 格式相同，
     * 共用此 function
     *
     * @return API Response status code, and body
     */
    public AbstractMap.SimpleEntry<Integer, String> upsertTopic(
            int communityId, int forumId, int topicId, String topicTitle,
            String fileContent, String action, String accessToken) {
        // fileName 格式為 ${topicId}.html
        String fileName = topicId + DRC_SYNC_DOT_HTML;
        // 如果文章內容重複 drc sync api 會無法上傳，
        // 所以fileContent前加上TopicId，後面加上目前時間
        long timestamp = Instant.now().toEpochMilli();
        String contentWithTimestamp = topicTitle + fileContent + timestamp;
        log.debug("drcAdapter contentWithTimestamp: " + contentWithTimestamp);
        byte[] data = contentWithTimestamp.getBytes();
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(DRC_SYNC_FILE, new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        // 準備 request header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        List<MediaType> mediaTypes = new ArrayList<>();
        mediaTypes.add(MediaType.APPLICATION_JSON);
        headers.setAccept(mediaTypes);
        headers.set(DRC_SYNC_AUTHORIZATION, DRC_SYNC_BEARER + accessToken);
        // 準備 request url
        String url = drcSyncConfig.getUrl() + UPSERT_URL;
        // 準備 request url parameter
        MultiValueMap<String, String> urlParams = new LinkedMultiValueMap<>();
        urlParams.add(DRC_SYNC_PROJECT_ID, drcSyncConfig.getProjectId());
        urlParams.add(DRC_SYNC_COLLECTION_ID, drcSyncConfig.getCollectionId());
        return handleRequest(url, HttpMethod.POST, headers, body, urlParams, action, topicId, communityId, forumId);
    }

    /**
     * delete 文章時，使用此function，
     * 將 ${topicId}.html 組成檔案當作request parameter，
     * 透過 handleRequest 拿到打API的結果，
     *
     * @return      API Response status code, and body
     */
    public AbstractMap.SimpleEntry<Integer, String> deleteTopic(int communityId, int forumId, int topicId, String action, String accessToken) {
        // fileName 格式為 ${topicId}.html
        String fileName = topicId + DRC_SYNC_DOT_HTML;
        // 準備 request url
        String url = drcSyncConfig.getUrl() + DELETE_URL;
        // 準備 request url parameter
        MultiValueMap<String, String> urlParams = new LinkedMultiValueMap<>();
        urlParams.add(DRC_SYNC_FILENAME, fileName);
        urlParams.add(DRC_SYNC_PROJECT_ID, drcSyncConfig.getProjectId());
        // 準備 request header
        HttpHeaders headers = new HttpHeaders();
        List<MediaType> mediaTypes = new ArrayList<>();
        mediaTypes.add(MediaType.APPLICATION_JSON);
        headers.setAccept(mediaTypes);
        headers.set(DRC_SYNC_AUTHORIZATION, DRC_SYNC_BEARER + accessToken);
        // 透過 handleRequest 拿到打API的結果
        return handleRequest(url, HttpMethod.DELETE, headers, null, urlParams, action, topicId, communityId, forumId);
    }

    private boolean isResponseSuccessful(ResponseEntity<?> response) {
        return response.getStatusCode() == HttpStatus.OK && response.getBody() != null;
    }

    /**
     * 使用此方法登入以取得 access_token
     * Route: /user/signin
     * Method: POST
     * Input: email, password
     */
    public DrcSyncSignIn signIn() {
        String url = drcSyncConfig.getUrl() + SIGNIN_URL;

        // Prepare the request body
        ObjectNode param = new ObjectMapper().createObjectNode();
        param.put(DRC_SYNC_EMAIL, drcSyncConfig.getEmail());
        param.put(DRC_SYNC_PASSWORD, drcSyncConfig.getPassword());
        DrcSyncSignIn drcSignIn = new DrcSyncSignIn();
        try {
            // Send the POST request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            List<MediaType> mediaTypes = new ArrayList<>();
            mediaTypes.add(MediaType.APPLICATION_JSON);
            headers.setAccept(mediaTypes);
            ResponseEntity<JsonNode> response = adapterUtil.sendRequest(
                    url,
                    HttpMethod.POST,
                    headers,
                    param.toString(),
                    null,
                    JsonNode.class);
            // 检查请求是否成功
            if (isResponseSuccessful(response)) {
                JsonNode responseBody = response.getBody();
                // Extract the user information
                DrcSyncUser drcUser = new DrcSyncUser();
                JsonNode userResponse = responseBody.get(DRC_SYNC_USER);
                drcUser.setId(userResponse.get(DRC_SYNC_ID).asText());
                drcUser.setUsername(userResponse.get(DRC_SYNC_USERNAME).asText());
                drcUser.setEmail(userResponse.get(DRC_SYNC_EMAIL).asText());
                drcUser.setRole(userResponse.get(DRC_SYNC_ROLE).asText());
                drcUser.setLevel(userResponse.get(DRC_SYNC_LEVEL).asInt());
                // Extract the token information
                DrcSyncTokens tokens = new DrcSyncTokens();
                JsonNode tokenResponse = responseBody.get(DRC_SYNC_TOKENS);
                tokens.setAccessToken(tokenResponse.get(DRC_SYNC_ACCESS_TOKEN).asText());
                tokens.setRefreshToken(tokenResponse.get(DRC_SYNC_REFRESH_TOKEN).asText());
                tokens.setTokenType(tokenResponse.get(DRC_SYNC_TOKEN_TYPE).asText());
                drcSignIn.setUser(drcUser);
                drcSignIn.setTokens(tokens);
            } else {
                log.debug("HTTP status: " + response.getStatusCodeValue());
                return null;
            }
        } catch (RestClientResponseException e) {
            log.debug("HTTP status: " + e.getRawStatusCode() + ", Response body: " + e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("An error occurred while signing in to DRC Sync: " + e.getMessage());
            return null;
        }
        return drcSignIn;
    }
}