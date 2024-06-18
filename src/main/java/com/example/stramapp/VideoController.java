package com.example.stramapp;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@CrossOrigin
@RestController
@RequestMapping("/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    private final Path videoLocation = Paths.get(System.getProperty("user.dir"), "video");

    @PostMapping("/upload")
    public ResponseEntity<String> uploadVideo(@RequestParam("file") MultipartFile file) {
        try {
            String m3u8Filename = videoService.uploadAndEncodeVideo(file);
            return ResponseEntity.ok(m3u8Filename);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to upload and encode video: " + e.getMessage());
        }
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getVideo(@PathVariable String filename) {
        try {
            Path file = videoLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            String contentType;

            if (filename.endsWith(".m3u8")) {
                contentType = "application/vnd.apple.mpegurl";
            } else if (filename.endsWith(".ts")) {
                contentType = "video/MP2T";
            } else {
                return ResponseEntity.badRequest().build();
            }

            CacheControl cacheControl = CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic();

            return ResponseEntity.ok()
                    .cacheControl(cacheControl)
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

}