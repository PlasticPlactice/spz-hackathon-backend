package com.example.demo.Service;

import com.example.demo.entity.DevLog;
import com.example.demo.repository.DevLogRepository;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DevLogService {
    private final DevLogRepository devLogRepository;

    public DevLogService(DevLogRepository devLogRepository) {
        this.devLogRepository = devLogRepository;
    }

    public DevLog createDevLog(DevLog devLog) {
        return devLogRepository.save(devLog);
    }

    public Optional<DevLog> getDevLogById(Long id) {
        return devLogRepository.findById(id);
    }

    public List<DevLog> getAllDevLogs() {
        return devLogRepository.findAll()
                .stream()
                .filter(log -> log.getDeletedAt() == null)
                .toList();
    }

    public DevLog updateDevLog(Long id, DevLog updatedDevLog) {
        return devLogRepository.findById(id).map(devLog -> {
            devLog.setDeletedAt(updatedDevLog.getDeletedAt());
            devLog.setUser(updatedDevLog.getUser());
            devLog.setLogDate(updatedDevLog.getLogDate());
            devLog.setKeepContent(updatedDevLog.getKeepContent());
            devLog.setProblemContent(updatedDevLog.getProblemContent());
            devLog.setTryContent(updatedDevLog.getTryContent());
            devLog.setGoalContent(updatedDevLog.getGoalContent());
            return devLogRepository.save(devLog);
        }).orElse(null);
    }

    public DevLog deleteDevLog(Long id) {
        return devLogRepository.findById(id).map(devLog -> {
            devLog.setDeletedAt(ZonedDateTime.now());
            return devLogRepository.save(devLog);
        }).orElse(null);
    }
}
