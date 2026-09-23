package com.orangehrm.utils;

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
 * Records the desktop during a test run and saves the .mov/.avi file under
 * reports/videos, satisfying the "Record video of the test run" requirement
 * when running Selenium (headed) locally.
 *
 * If a display/screen is not available (e.g. a headless CI container),
 * recording is silently skipped so the test suite still runs -- video
 * capture is a nice-to-have artifact, not a hard functional dependency.
 */
public class ScreenRecorderUtil {

    private static ScreenRecorder screenRecorder;

    public static void startRecording(String testName) {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                System.out.println("[ScreenRecorder] Headless environment detected - skipping video capture for " + testName);
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

            screenRecorder = new ScreenRecorder(gc,
                    gc.getBounds(),
                    new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_AVI),
                    new Format(MediaTypeKey, MediaType.VIDEO, EncodingKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                            CompressorNameKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                            DepthKey, 24, FrameRateKey, Rational.valueOf(15),
                            QualityKey, 1.0f, KeyFrameIntervalKey, 15 * 60),
                    new Format(MediaTypeKey, MediaType.VIDEO, EncodingKey, "black",
                            FrameRateKey, Rational.valueOf(30)),
                    null,
                    videoFolder);

            screenRecorder.start();
            System.out.println("[ScreenRecorder] Recording started for test: " + testName);
        } catch (Exception e) {
            System.out.println("[ScreenRecorder] Could not start recording (" + e.getMessage() + "). Continuing without video.");
        }
    }

    public static void stopRecording() {
        try {
            if (screenRecorder != null) {
                screenRecorder.stop();
                System.out.println("[ScreenRecorder] Recording stopped and saved under reports/videos");
            }
        } catch (IOException e) {
            System.out.println("[ScreenRecorder] Error stopping recording: " + e.getMessage());
        }
    }
}
