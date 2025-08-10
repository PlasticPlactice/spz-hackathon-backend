package com.example.demo.Controller;

import com.example.demo.Service.DevLogService;
import com.example.demo.entity.DevLog;
import org.springframework.http.MediaType; // MediaTypeをインポート
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 開発ログ情報を操作するためのREST APIコントローラー。
 */
@RestController
@RequestMapping("/api/devlogs")
@CrossOrigin(origins = "http://localhost:3000")
public class DevLogController {

    private final DevLogService devLogService;

    public DevLogController(DevLogService devLogService) {
        this.devLogService = devLogService;
    }

    // CREATE: 新しい開発ログを作成するAPI
    // produces属性を追加して、JSONを返すことを明記
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public DevLog createDevLog(@RequestBody DevLog devLog) {
        return devLogService.createDevLog(devLog);
    }

    // READ: 全ての開発ログを取得するAPI
    // produces属性を追加して、JSONを返すことを明記
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<DevLog> getAllDevLogs() {
        return devLogService.getAllDevLogs();
    }
}