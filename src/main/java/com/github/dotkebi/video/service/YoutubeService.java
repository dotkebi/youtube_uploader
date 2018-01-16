package com.github.dotkebi.video.service;

import com.github.dotkebi.video.property.YoutubeProperty;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * @author by dotkebi on 2018. 1. 10..
 */
@Service
public class YoutubeService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private static final String CREDENTIALS_DIRECTORY = ".oauth-credentials";

    @Autowired
    private YoutubeProperty youtubeProperty;

    private YouTube youtube;

    @PostConstruct
    public void init() throws Exception {
        youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, authorize()).setApplicationName("video-upload")
                .build();
    }

    private Credential authorize() throws Exception {
        URL url = getClass().getResource("/youtube-upload.p12");
        File file = new File(url.getFile());
        return new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId("youtube-pem")
                .setServiceAccountPrivateKeyFromP12File(file)
                .setServiceAccountScopes(Collections.singleton(YouTubeScopes.YOUTUBE_UPLOAD))
                .setServiceAccountUser(youtubeProperty.getUser())
                .build();
    }

    public String upload(MultipartFile multipartFile) {

        Video videoObjectDefiningMetadata = new Video();

        VideoStatus status = new VideoStatus();
        status.setPrivacyStatus("public");
        videoObjectDefiningMetadata.setStatus(status);

        VideoSnippet snippet = new VideoSnippet();
        Calendar cal = Calendar.getInstance();
        snippet.setTitle("Test Upload via Java on " + cal.getTime());
        snippet.setDescription(
                "Video uploaded via YouTube Data API V3 using the Java library " + "on " + cal.getTime());

        // Set your keywords.
        List<String> tags = new ArrayList<>();
        tags.add("test");
        tags.add("example");
        tags.add("java");
        tags.add("YouTube Data API V3");
        tags.add("erase me");
        snippet.setTags(tags);

        // Set completed snippet to the video object.
        videoObjectDefiningMetadata.setSnippet(snippet);
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(multipartFile.getInputStream())) {
            InputStreamContent mediaContent = new InputStreamContent(
                    "video/*", bufferedInputStream);
            mediaContent.setLength(multipartFile.getSize());

            YouTube.Videos.Insert videoInsert = youtube.videos()
                    .insert("snippet,statistics,status", videoObjectDefiningMetadata, mediaContent);

            MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();

            /*
             * Sets whether direct media upload is enabled or disabled. True = whole media content is
             * uploaded in a single request. False (default) = resumable media upload protocol to upload
             * in data chunks.
             */
            uploader.setDirectUploadEnabled(false);

            MediaHttpUploaderProgressListener progressListener = uploader1 -> {
                switch (uploader1.getUploadState()) {
                    case INITIATION_STARTED:
                        log.info("Initiation Started");
                        break;
                    case INITIATION_COMPLETE:
                        log.info("Initiation Completed");
                        break;
                    case MEDIA_IN_PROGRESS:
                        log.info("Upload in progress");
                        log.info("Upload percentage: " + uploader1.getProgress());
                        break;
                    case MEDIA_COMPLETE:
                        log.info("Upload Completed!");
                        break;
                    case NOT_STARTED:
                        log.info("Upload Not Started!");
                        break;
                }
            };
            uploader.setProgressListener(progressListener);

            // Execute upload.
            Video returnedVideo = videoInsert.execute();

            return returnedVideo.getId();


        } catch (IOException e) {
            log.error(e.getMessage());
        }

        throw new IllegalArgumentException();
    }

}
