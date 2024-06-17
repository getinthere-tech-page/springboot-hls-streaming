package com.example.stramapp;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        logger.info("Starting FFmpegFrameGrabber for input file: " + inputFilePath);
        // FFmpegFrameGrabber로 입력 파일 읽기
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFilePath)) {
            grabber.start();
            logger.info("FFmpegFrameGrabber started");

            // 스트림 정보 로그 출력
            logger.info("Number of Streams: " + grabber.getLengthInFrames());

            // 총 프레임 수와 프레임 레이트를 로그로 출력
            int totalFrames = grabber.getLengthInFrames();
            double frameRate = grabber.getFrameRate();
            double durationInSeconds = totalFrames / frameRate;
            logger.info("Total Frames: " + totalFrames);
            logger.info("Frame Rate: " + frameRate);
            logger.info("Duration (seconds): " + durationInSeconds);

            // FFmpegFrameRecorder로 출력 파일 생성
            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFilePath, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels())) {
                recorder.setFormat("hls");
                recorder.setOption("hls_time", "10");
                recorder.setOption("hls_list_size", "0");
                recorder.setOption("hls_flags", "split_by_time");
                recorder.setOption("hls_wrap", "0");
                recorder.setOption("loglevel", "debug");

                // 코덱 설정
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                recorder.setFrameRate(grabber.getFrameRate());
                recorder.setSampleRate(grabber.getSampleRate());
                recorder.setAudioChannels(grabber.getAudioChannels());

                // 비디오 및 오디오 비트레이트 설정
                recorder.setVideoBitrate(grabber.getVideoBitrate());
                recorder.setAudioBitrate(grabber.getAudioBitrate());
                //recorder.setPixelFormat(0);

                // 비디오 및 오디오 스트림 매핑
                recorder.start();
                logger.info("FFmpegFrameRecorder started");

                // 프레임을 읽어서 기록
                Frame frame;
                int frameNumber = 0;
                while ((frame = grabber.grabFrame()) != null) {
                    frameNumber++;
                    if (frame.image != null) {
                        recorder.record(frame);
                        //logger.info("Recorded video frame: " + frameNumber);
                    } else if (frame.samples != null && frame.samples.length > 0) {
                        recorder.recordSamples(frame.sampleRate, frame.audioChannels, frame.samples);
                        //logger.info("Recorded audio frame: " + frameNumber);
                    } else {
                        //logger.warning("Skipped frame: " + frameNumber);
                    }
                }

                // 자원 해제
                recorder.stop();
                grabber.stop();
                logger.info("Recording and grabbing stopped");

            } catch (Exception e) {
                logger.severe("비디오 인코딩 중 오류가 발생했습니다: " + e.getMessage());
                throw new IOException("비디오 인코딩 중 오류가 발생했습니다: " + e.getMessage(), e);
            }
        }

        return baseName + ".m3u8";
    }
}