#!/bin/bash

VIDEO_DIRECTORY=~/Documents/Experience_videos/
FILE_NAME=test.avi

## libcamera-vid

# UDP
#vlc udp://@:1234 :demux=h264 # Work with libcamera-vid - l # Work , Quality : correct, Latency :
#ffplay udp://192.168.111.151:1234 -fflags nobuffer -flags low_delay -framedrop # libcamera-vid udp Work, Quality : correct, Latency : 4s

# TCP
#cvlc tcp/h264://192.168.111.151:1234 # Work, Quality : bad, Latency : >4s, glconv_vaapi_drm gl error: vaInitialize: unknown libva error
#ffplay tcp://192.168.111.151:1234 -vf "setpts=N/60" -fflags nobuffer -flags low_delay -framedrop # libcamera-vid tcp Work, Quality : good, Latency : <1s

# RTSP
#vlc rtsp://192.168.111.151:8554/stream1 #  Work, Quality : bad, Latency : 5s, main decoder error: buffer deadlock prevented
#ffplay rtsp://192.168.111.151:8554/stream1 -vf "setpts=N/60" -fflags nobuffer -flags low_delay -framedrop #  Work, Quality : bof, Latency : >3,5s

## libcamera-vid

# UDP
#vlc udp://@:1234 :demux=h264 # Work, Quality : bad, Latency : >2s, errors
#ffplay udp://192.168.111.151:1234 -fflags nobuffer -flags low_delay -framedrop # Work, Quality : good, Latency : >4,5s

# TCP
#cvlc tcp/h264://192.168.111.151:1234 # Work, Quality : bad, Latency : >2s, errors :

#libva info: Trying to open /usr/lib/x86_64-linux-gnu/dri/nvidia_drv_video.so
#libva info: va_openDriver() returns -1
#[00007f7eec002bd0] glconv_vaapi_x11 gl error: vaInitialize: unknown libva error

rm test.avi
ffmpeg -loglevel verbose -fflags nobuffer -flags low_delay -i tcp://192.168.111.151:1234 -b:v 10M -an -sn -r 30 -vcodec copy $VIDEO_DIRECTORY$FILE_NAME # Work, Quality : good, Latency : <1s
#ffplay tcp://192.168.111.151:1234 -fs -fast -loglevel verbose -vf "setpts=N/40" -fflags nobuffer -flags low_delay -framedrop # Work, Quality : good, Latency : <1s
