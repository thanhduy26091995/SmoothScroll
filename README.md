# SmoothScroll

SmoothScroll is an Android app designed to provide a smooth video scrolling experience, similar to TikTok or YouTube Shorts, but with a focus on performance, caching, and prefetching. The app allows users to preview video thumbnails while seeking through the video and includes additional features for adaptive streaming and video download.
I used Media3 ExoPlayer to achieve video playback

# Video Demo
https://github.com/user-attachments/assets/c6e1bf4d-3558-451c-8b8e-ad9b8045a692

## Features

- **Smooth Scrolling Performance:** Designed to handle fast scrolling with optimized performance.
- **Thumbnail Preview:** View video thumbnails while seeking to different durations in the video.
- **Caching and Prefetching:** Efficiently caches and prefetches video and thumbnail data to enhance user experience and reduce loading times.

## Upcoming Features

- **Adaptive Video Streaming:** Adjust video quality based on network conditions to provide a better streaming experience.
- **Video Preview during Adaptive Streaming:** Preview video content while watching adaptive video streams.
- **Video Download:** Ability to download videos for offline viewing.
- **Configurable Playback Behavior:** Customize video playback settings such as auto-play and buffering strategies.

## Video Playback and Thumbnail Preview

When seeking through a video, the app provides a preview of the video's thumbnail to give users an idea of the content at the selected point. The thumbnails are preloaded and cached to ensure quick display.

## API Response

The app expects responses from the server (Fetched from JSON file) in the following format:

```json
{
    "status": "success",
    "video_id": "24a0033e-0e0c-42c4-9f3a-1f0ce9d1a1d8",
    "processing_status": "completed",
    "video_url": "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_1.mp4",
    "metadata": {
        "duration": 25000,
        "width": 480.0,
        "height": 360.0,
        "bitrate": "413kbps",
        "codec": "H.264"
    },
    "thumbnails": {
        "small": [
            {
                "thumbnailUrl": "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/thumbnails/video_1/small/thumbnail_0.jpg",
                "time": 0
            },
            // more thumbnail objects
        ],
        "medium": [
            {
                "thumbnailUrl": "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/thumbnails/video_1/medium/thumbnail_0.jpg",
                "time": 0
            },
            // more thumbnail objects
        ]
    },
    "previews": []
}
```

- status: Indicates the success or failure of the request.
- video_id: Unique identifier for the video.
- processing_status: Current processing status of the video.
- video_url: URL where the video is hosted.
- metadata: Contains details about the video such as duration, width, height, bitrate, and codec.
- thumbnails: Provides URLs for thumbnails of the video at various intervals and sizes (small, medium).
- previews: Additional preview information (currently not used in this version, will support for DASH and HLS format).
