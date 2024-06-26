package com.example.hls.service;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class HlsService {

    public Resource getVideoRes(String tsFile) throws IOException {
        File file = new File(getConvertVideoPath(tsFile));
        return new FileSystemResource(file.getCanonicalPath());
    }

    private String getRawVideoPath(String videoFileName) {
        return "src/main/resources/file/" + videoFileName;
    }

    private String getConvertVideoPath(String outputFileName) {
        return "src/main/resources/convert/" + outputFileName;
    }

    public void convertHls() throws IOException {
        String videoFilePath = new File(getRawVideoPath("good.mp4")).getCanonicalPath();
        String convertPath =  new File(getConvertVideoPath("/good.m3u8")).getCanonicalPath();

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(videoFilePath)
                .addOutput(convertPath)
                .addExtraArgs("-profile:v", "baseline")
                .addExtraArgs("-level", "3.0")
                .addExtraArgs("-start_number", "0")
                .addExtraArgs("-hls_time", "10")
                .addExtraArgs("-hls_list_size", "0")
                .addExtraArgs("-f", "hls")
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(new FFmpeg(), new FFprobe());
        executor.createJob(builder).run();
    }
}
