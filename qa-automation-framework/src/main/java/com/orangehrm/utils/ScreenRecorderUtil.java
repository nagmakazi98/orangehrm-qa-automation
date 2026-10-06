package com.orangehrm.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monte.media.Format;
import org.monte.media.FormatKeys.MediaType;
import org.monte.media.math.Rational;
import org.monte.screenrecorder.ScreenRecorder;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import static org.monte.media.FormatKeys.*;
import static org.monte.media.VideoFormatKeys.*;

/**
 * Records the desktop during a test run and saves an {@code .avi} file under
 * {@code reports/videos}.
 *
 * <p>If a display/screen is not available (headless CI containers),
 * recording is silently skipped — the suite still runs normally.</p>
 */
public class ScreenRecorderUtil {

    private static final Logger log = LogManager.getLogger(ScreenRecorderUtil.class);
    private static ScreenRecorder screenRecorder;

    public static void startRecording(String testName) {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                log.info("[ScreenRecorder] Headless environment — skipping video capture for '{}'", testName);
                return;
            }

            File videoFolder = new File(ConfigReader.get("video.dir", "reports/videos"));
            if (!videoFolder.exists()) {
                videoFolder.mkdirs();
            }

            GraphicsConfiguration gc = GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration();

            screenRecorder = new ScreenRecorder(gc, gc.getBounds(),
                    new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_AVI),
                    new Format(MediaTypeKey, MediaType.VIDEO,
                            EncodingKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                            CompressorNameKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                            DepthKey, 24, FrameRateKey, Rational.valueOf(15),
                            QualityKey, 1.0f, KeyFrameIntervalKey, 15 * 60),
                    new Format(MediaTypeKey, MediaType.VIDEO,
                            EncodingKey, "black", FrameRateKey, Rational.valueOf(30)),
                    null, videoFolder);

            screenRecorder.start();
            log.info("[ScreenRecorder] Recording started for '{}'", testName);
        } catch (Exception e) {
            log.warn("[ScreenRecorder] Could not start recording ({}). Continuing without video.",
                    e.getMessage());
        }
    }

    public static void stopRecording() {
        try {
            if (screenRecorder != null) {
                screenRecorder.stop();
                log.info("[ScreenRecorder] Recording stopped and saved to reports/videos");
            }
        } catch (IOException e) {
            log.error("[ScreenRecorder] Error stopping recording: {}", e.getMessage());
        }
    }
}
