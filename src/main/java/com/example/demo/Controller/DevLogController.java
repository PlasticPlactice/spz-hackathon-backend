package com.example.demo.Controller;

import com.example.demo.Service.DevLogService;
import com.example.demo.entity.DevLog;
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
    @PostMapping
    public DevLog createDevLog(@RequestBody DevLog devLog) {
        return devLogService.createDevLog(devLog);
    }

    // READ: 全ての開発ログを取得するAPI
    @GetMapping
    public List<DevLog> getAllDevLogs() {
        return devLogService.getAllDevLogs();
    }

    // READ: IDを指定して開発ログを検索するAPI
    @GetMapping("/{id}")
    public ResponseEntity<DevLog> getDevLogById(@PathVariable Long id) {
        return devLogService.getDevLogById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // UPDATE: 既存の開発ログを更新するAPI
    @PutMapping("/{id}")
    public ResponseEntity<DevLog> updateDevLog(@PathVariable Long id, @RequestBody DevLog devLog) {
        DevLog updatedDevLog = devLogService.updateDevLog(id, devLog);
        if (updatedDevLog != null) {
            return ResponseEntity.ok(updatedDevLog);
        }
        return ResponseEntity.notFound().build();
    }

    // DELETE: IDを指定して開発ログを論理削除するAPI
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevLog(@PathVariable Long id) {
        DevLog deletedDevLog = devLogService.deleteDevLog(id);
        if (deletedDevLog != null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
