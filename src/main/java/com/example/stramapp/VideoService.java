package com.example.stramapp;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class VideoService {

    private final Path videoLocation = Paths.get(System.getProperty("user.dir"), "video");
    private static final Logger logger = Logger.getLogger(VideoService.class.getName());

    public VideoService() {
        // Ensure the video directory exists
        try {
            Files.createDirectories(videoLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create video directory", e);
        }

        // Set FFmpeg log callback
        FFmpegLogCallback.set();
    }

    public String uploadAndEncodeVideo(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("파일 이름이 비어 있습니다.");
        }

        String baseName = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
        String inputFilePath = videoLocation.resolve(originalFilename).toString();
        String outputFilePath = videoLocation.resolve(baseName + ".m3u8").toString();

        // 파일 저장
        File inputFile = new File(inputFilePath);
        file.transferTo(inputFile);

        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");
        command.add(inputFilePath);
        command.add("-codec:v");
        command.add("libx264");
        command.add("-codec:a");
        command.add("aac");
        command.add("-strict");
        command.add("-2");
        command.add("-start_number");
        command.add("0");
        command.add("-hls_time");
        command.add("10");
        command.add("-hls_list_size");
        command.add("0");
        command.add("-force_key_frames");
        command.add("expr:gte(t,n_forced*10)");
        command.add("-f");
        command.add("hls");
        command.add(outputFilePath);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("HLS encoding completed successfully.");
            } else {
                System.out.println("HLS encoding failed with exit code " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return baseName + ".m3u8";
    }
}