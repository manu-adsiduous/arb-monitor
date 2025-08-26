package com.arbmonitor.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for analyzing video content including:
 * - Frame extraction from videos
 * - OCR text extraction from frames
 * - Audio transcription from video audio tracks
 * - Combining all extracted text for compliance analysis
 */
@Service
public class VideoAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(VideoAnalysisService.class);
    
    @Value("${app.media.base-path:./media}")
    private String mediaBasePath;
    
    @Value("${app.video.frame-interval:0.5}")
    private double frameIntervalSeconds;
    
    @Value("${app.video.max-frames:120}")
    private int maxFramesToExtract;
    
    @Value("${app.video.cleanup-frames:true}")
    private boolean cleanupFrames;
    
    /**
     * Analyze a video file and extract all text content
     * @param videoPath Path to the local video file
     * @return VideoAnalysisResult containing all extracted text
     */
    public VideoAnalysisResult analyzeVideo(String videoPath) {
        logger.info("Starting video analysis for: {}", videoPath);
        
        VideoAnalysisResult result = new VideoAnalysisResult();
        result.setVideoPath(videoPath);
        
        try {
            // Validate video file exists
            File videoFile = new File(videoPath);
            if (!videoFile.exists()) {
                throw new IllegalArgumentException("Video file not found: " + videoPath);
            }
            
            // Create temporary directory for processing
            String tempDir = createTempDirectory(videoPath);
            
            // Step 1: Extract frames from video
            List<String> framePaths = extractFrames(videoPath, tempDir);
            logger.info("Extracted {} frames from video", framePaths.size());
            
            // Step 2: Run OCR on each frame
            List<String> frameTexts = extractTextFromFrames(framePaths);
            result.setFrameTexts(frameTexts);
            
            // Step 3: Extract and transcribe audio
            String audioTranscript = extractAndTranscribeAudio(videoPath, tempDir);
            result.setAudioTranscript(audioTranscript);
            
            // Step 4: Combine all text
            String combinedText = combineAllText(frameTexts, audioTranscript);
            result.setCombinedText(combinedText);
            
            // Cleanup temporary files
            cleanupTempDirectory(tempDir);
            
            logger.info("Video analysis completed. Combined text length: {} characters", 
                       combinedText.length());
            
        } catch (Exception e) {
            logger.error("Error analyzing video: {}", videoPath, e);
            result.setError("Video analysis failed: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Extract frames from video at regular intervals using FFmpeg
     */
    private List<String> extractFrames(String videoPath, String tempDir) throws IOException, InterruptedException {
        logger.info("Extracting frames from video: {}", videoPath);
        
        List<String> framePaths = new ArrayList<>();
        
        // FFmpeg command to extract frames every N seconds
        String[] command = {
            "ffmpeg",
            "-i", videoPath,
            "-vf", String.format("fps=%.2f", 1.0 / frameIntervalSeconds), // Extract frames at calculated FPS
            "-frames:v", String.valueOf(maxFramesToExtract), // Limit number of frames
            "-y", // Overwrite output files
            tempDir + "/frame_%04d.jpg"
        };
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // Log FFmpeg output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("FFmpeg: {}", line);
            }
        }
        
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("FFmpeg frame extraction timed out");
        }
        
        if (process.exitValue() != 0) {
            throw new RuntimeException("FFmpeg frame extraction failed with exit code: " + process.exitValue());
        }
        
        // Collect extracted frame paths
        File tempDirFile = new File(tempDir);
        File[] frameFiles = tempDirFile.listFiles((dir, name) -> name.startsWith("frame_") && name.endsWith(".jpg"));
        
        if (frameFiles != null) {
            Arrays.sort(frameFiles); // Sort by filename to maintain order
            for (File frameFile : frameFiles) {
                framePaths.add(frameFile.getAbsolutePath());
            }
        }
        
        logger.info("Successfully extracted {} frames", framePaths.size());
        return framePaths;
    }
    
    /**
     * Extract text from video frames using Tesseract OCR
     */
    private List<String> extractTextFromFrames(List<String> framePaths) {
        logger.info("Running OCR on {} frames", framePaths.size());
        
        List<String> frameTexts = new ArrayList<>();
        
        for (int i = 0; i < framePaths.size(); i++) {
            String framePath = framePaths.get(i);
            try {
                String extractedText = runOcrOnImage(framePath);
                frameTexts.add(extractedText);
                
                if (!extractedText.trim().isEmpty()) {
                    logger.debug("Frame {}: extracted {} characters of text", i + 1, extractedText.length());
                }
                
            } catch (Exception e) {
                logger.warn("Failed to extract text from frame {}: {}", framePath, e.getMessage());
                frameTexts.add(""); // Add empty string to maintain index alignment
            } finally {
                // Clean up frame file after processing if cleanup is enabled
                if (cleanupFrames) {
                    try {
                        Files.deleteIfExists(Paths.get(framePath));
                        logger.debug("Cleaned up frame: {}", framePath);
                    } catch (Exception e) {
                        logger.warn("Failed to cleanup frame {}: {}", framePath, e.getMessage());
                    }
                }
            }
        }
        
        logger.info("OCR completed on {} frames", frameTexts.size());
        return frameTexts;
    }
    
    /**
     * Run Tesseract OCR on a single image
     */
    private String runOcrOnImage(String imagePath) throws IOException, InterruptedException {
        String[] command = {
            "tesseract",
            imagePath,
            "stdout", // Output to stdout instead of file
            "-l", "eng", // English language
            "--psm", "6" // Assume uniform block of text
        };
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Tesseract OCR timed out");
        }
        
        if (process.exitValue() != 0) {
            throw new RuntimeException("Tesseract OCR failed with exit code: " + process.exitValue());
        }
        
        return output.toString().trim();
    }
    
    /**
     * Extract audio from video and transcribe to text
     */
    private String extractAndTranscribeAudio(String videoPath, String tempDir) throws IOException, InterruptedException {
        logger.info("Extracting and transcribing audio from video");
        
        // Step 1: Extract audio using FFmpeg
        String audioPath = tempDir + "/audio.wav";
        String[] extractCommand = {
            "ffmpeg",
            "-i", videoPath,
            "-vn", // No video
            "-acodec", "pcm_s16le", // PCM 16-bit little-endian
            "-ar", "16000", // 16kHz sample rate (good for speech recognition)
            "-ac", "1", // Mono
            "-y", // Overwrite output
            audioPath
        };
        
        ProcessBuilder pb = new ProcessBuilder(extractCommand);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // Log FFmpeg output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("FFmpeg audio: {}", line);
            }
        }
        
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Audio extraction timed out");
        }
        
        if (process.exitValue() != 0) {
            logger.warn("Audio extraction failed or no audio track found");
            return ""; // Return empty string if no audio
        }
        
        // Step 2: Transcribe audio (placeholder - will implement with Whisper or cloud service)
        return transcribeAudio(audioPath);
    }
    
    /**
     * Transcribe audio file to text
     * TODO: Implement with OpenAI Whisper or Google Speech-to-Text
     */
    private String transcribeAudio(String audioPath) {
        logger.info("Transcribing audio file: {}", audioPath);
        
        File audioFile = new File(audioPath);
        if (!audioFile.exists() || audioFile.length() == 0) {
            logger.info("No audio file to transcribe");
            return "";
        }
        
        // Placeholder implementation
        // TODO: Integrate with Whisper API or Google Speech-to-Text
        logger.info("Audio transcription not yet implemented - returning placeholder");
        return "[Audio transcription will be implemented with Whisper/Google Speech-to-Text]";
    }
    
    /**
     * Combine all extracted text into a single string for analysis
     */
    private String combineAllText(List<String> frameTexts, String audioTranscript) {
        StringBuilder combined = new StringBuilder();
        
        // Add frame texts
        for (int i = 0; i < frameTexts.size(); i++) {
            String frameText = frameTexts.get(i);
            if (!frameText.trim().isEmpty()) {
                combined.append("FRAME ").append(i + 1).append(":\n");
                combined.append(frameText.trim()).append("\n\n");
            }
        }
        
        // Add audio transcript
        if (audioTranscript != null && !audioTranscript.trim().isEmpty()) {
            combined.append("AUDIO TRANSCRIPT:\n");
            combined.append(audioTranscript.trim()).append("\n\n");
        }
        
        return combined.toString();
    }
    
    /**
     * Create temporary directory for video processing
     */
    private String createTempDirectory(String videoPath) throws IOException {
        String videoName = Paths.get(videoPath).getFileName().toString();
        String baseName = videoName.substring(0, videoName.lastIndexOf('.'));
        
        Path tempDir = Paths.get(mediaBasePath, "temp", "video_analysis", baseName + "_" + System.currentTimeMillis());
        Files.createDirectories(tempDir);
        
        return tempDir.toString();
    }
    
    /**
     * Clean up temporary directory and files
     */
    private void cleanupTempDirectory(String tempDir) {
        try {
            Path tempPath = Paths.get(tempDir);
            if (Files.exists(tempPath)) {
                Files.walk(tempPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
                logger.debug("Cleaned up temporary directory: {}", tempDir);
            }
        } catch (Exception e) {
            logger.warn("Failed to cleanup temporary directory: {}", tempDir, e);
        }
    }
    
    /**
     * Result class for video analysis
     */
    public static class VideoAnalysisResult {
        private String videoPath;
        private List<String> frameTexts = new ArrayList<>();
        private String audioTranscript = "";
        private String combinedText = "";
        private String error;
        
        // Getters and setters
        public String getVideoPath() { return videoPath; }
        public void setVideoPath(String videoPath) { this.videoPath = videoPath; }
        
        public List<String> getFrameTexts() { return frameTexts; }
        public void setFrameTexts(List<String> frameTexts) { this.frameTexts = frameTexts; }
        
        public String getAudioTranscript() { return audioTranscript; }
        public void setAudioTranscript(String audioTranscript) { this.audioTranscript = audioTranscript; }
        
        public String getCombinedText() { return combinedText; }
        public void setCombinedText(String combinedText) { this.combinedText = combinedText; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public boolean hasError() { return error != null; }
        
        public boolean hasText() { 
            return combinedText != null && !combinedText.trim().isEmpty(); 
        }
    }
}
