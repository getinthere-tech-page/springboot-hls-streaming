package com.example.stramapp;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        System.out.println(1);
        try {
            System.out.println(2);
            String m3u8Filename = videoService.uploadAndEncodeVideo(file);
            System.out.println(3);
            return ResponseEntity.ok(m3u8Filename);
        } catch (Exception e) {
            System.out.println(4);
            return ResponseEntity.status(500).body("Failed to upload and encode video: " + e.getMessage());
        }
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getVideo(@PathVariable String filename) {
        try {
            Path file = videoLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            String contentType = "application/vnd.apple.mpegurl";

            if (filename.endsWith(".ts")) {
                contentType = "video/MP2T";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}