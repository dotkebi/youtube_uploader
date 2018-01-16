package com.github.dotkebi.video.controllers;

import com.github.dotkebi.video.service.YoutubeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
public class UploadController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private YoutubeService youtubeService;

    @PostMapping("/upload")
    public ResponseEntity uploadVideo(
            @RequestPart(value = "attachment") MultipartFile multipartFile
    ) {
        log.info("/upload, [{}]", multipartFile.getOriginalFilename());

        // upload youtube
        String youtube_url = youtubeService.upload(multipartFile);

        return ResponseEntity.ok(youtube_url);
    }

    @GetMapping("/oauth2callback")
    public ResponseEntity callback(
            @RequestParam String code
    ) {
        log.info("auth : {}", code);
        return ResponseEntity.ok().build();
    }

}
