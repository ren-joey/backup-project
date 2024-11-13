package com.delta.dms.community.controller;

/**
 *  處理測試需要的邏輯:
 *    tag = Test
 *      /testLanguage/organization/uiapi/org/{orgId} (GET)
 */


import com.delta.datahive.searchobj.param.GeneralQuery;
import com.delta.datahive.searchobj.param.Query;
import com.delta.datahive.searchobj.response.SearchResponse;
import com.delta.datahive.searchobj.type.activitylog.ActivityLogResult;
import com.delta.dms.community.adapter.SearchAdapter;
import com.delta.dms.community.service.ForumService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;



@Api(
        value = "Test",
        tags = {"Test"},
        description = "For Test"
)
@RestController
public class TestController {

    @GetMapping("/testTemplate")
    public ResponseEntity<String> testTemplate(
            @RequestParam(required = false) String param1,
            @RequestParam(required = false) String param2) {
        return ResponseEntity.ok(param1 + param2);
    }
}