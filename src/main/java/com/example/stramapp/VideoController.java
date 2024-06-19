package com.example.stramapp;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

// 참고하기
// https://github.com/joejoe2/spring-video/blob/main/src/main/java/com/joejoe2/video/controller/video/VideoController.java
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

    @GetMapping("/{filename}.m3u8")
    public ResponseEntity<?> videoHlsM3U8(@PathVariable String filename) {
        Path file = videoLocation.resolve(filename);
        try{
            Resource resource = new UrlResource(file.toUri());

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + ".m3u8");
            headers.setContentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"));
            return new ResponseEntity<Resource>(resource, headers, HttpStatus.OK);
        }catch (Exception e){
            e.printStackTrace();
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{tsname}.ts")
    public ResponseEntity<?> videoHlsTs(@PathVariable String tsname) {
        Path file = videoLocation.resolve(tsname);

        try{
            Resource resource = new UrlResource(file.toUri());

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + tsname + ".ts");
            headers.setContentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE));
            return new ResponseEntity<Resource>(resource, headers, HttpStatus.OK);
        }catch (Exception e){
            e.printStackTrace();
        }
        return ResponseEntity.badRequest().build();


    }


    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getVideo(@PathVariable String filename) {
        System.out.println("파일네임 요청옴 : "+filename);
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