package com.luckyun.stock.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.luckyun.tools.upload.dto.FtpUploadResult;
import com.luckyun.tools.upload.service.FtpUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SaCheckLogin
public class FileController {

    private final FtpUploadService ftpUploadService;

    /**
     * 文件上传接口（通过 FTP 存储）
     *
     * @param file 上传的文件
     * @return {code:200, msg:"success", data:{url, fileName, size}}
     */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "msg", "上传文件不能为空"));
        }

        FtpUploadResult result = ftpUploadService.upload(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getSize()
        );

        if (result.url() == null) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("code", 500, "msg", "文件已上传但未配置访问URL，请检查 ftp.url-prefix"));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("url", result.url());
        data.put("fileName", result.fileName());
        data.put("ftpPath", result.ftpPath());
        data.put("size", result.size());

        Map<String, Object> body = new HashMap<>();
        body.put("code", 200);
        body.put("msg", "success");
        body.put("data", data);
        return ResponseEntity.ok(body);
    }
}
