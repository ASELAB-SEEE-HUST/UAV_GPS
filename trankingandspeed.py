import time
import cv2
import torch
from ASE_Detect_yolov7 import Detect
import xpc
import math
import numpy as np


# Hàm thiết lập gimbal và các thông số liên quan
def setup_gimbal(gimbal_yaw, gimbal_pitch, au_altitude):
    with xpc.XPlaneConnect() as client:
        # Thiết lập các giá trị cho gimbal và các thông số khác
        ase_yaw = "ASELab/ase_yaw"
        ase_pitch = "ASELab/ase_pitch"
        ase_custom_1 = "ASELab/ase_custom_1"
        ase_custom_2 = "ASELab/ase_custom_2"
        ase_custom_4 = "ASELab/ase_custom_4"
        ase_object_alt = "ASELab/ase_object_alt"
        khai_velocityX = "ASELab/ase_custom_6"
        khai_velocityY = "ASELab/ase_custom_5"
        khai_obj_lat = "ASELab/ase_object_lat"
        khai_obj_lng = "ASELab/ase_object_lng"
        
        # Gửi các giá trị tới X-Plane để điều khiển gimbal
        client.sendDREF(khai_velocityX, 0)
        client.sendDREF(khai_velocityY, 800)
        client.sendDREF(ase_object_alt, au_altitude)
        client.sendDREF(ase_pitch, gimbal_pitch)
        client.sendDREF(ase_yaw, gimbal_yaw)
        client.sendDREF(ase_custom_1, 1.5)
        client.sendDREF(ase_custom_2, 3.5)
        client.sendDREF(ase_custom_4, 2.6)

if __name__ == "__main__":
    # Thiết lập thông số cho gimbal
    au_altitude = 40
    gimbal_pitch = -20
    gimbal_yaw = 0
    frame_height = 640
    frame_width = 960

    # Thiết lập gimbal và thông số liên quan
    setup_gimbal(gimbal_yaw, gimbal_pitch, au_altitude)

    # Thiết lập mô hình detect
    weights = "best.pt"
    Det = Detect(weights, 640, 'cpu')

    # # Khởi tạo cửa sổ hiển thị
    # vid = WindowCapture("X-System")

    # # Khởi tạo luồng RTSP
    # rtsp_url = "rtsp://anhdz:123@192.168.88.6:8554/streaming/live/1"
    # cap = cv2.VideoCapture(rtsp_url)

    # Đường dẫn đến video đã có sẵn
    video_path = "test.mp4"
    cap = cv2.VideoCapture(video_path)

    # Khởi tạo các biến liên quan đến tracking_objects, object_positions, object_timestamps và track_id
    tracking_objects = {}
    object_positions = {}
    object_timestamps = {}
    track_id = 0
    detect_flag = False

    # Vòng lặp chính
    while True:
        # frame = vid.get_screenshot()
        # if frame is None:
        #     break

        ret, frame = cap.read()
        if not ret:
            print("Error: Failed to read frame from RTSP stream")
            break

            # Phát hiện đối tượng
        detections = Det.detect(frame, conf_thres=0.25, iou_thres=0.45)
        print(type(detections))
            # Xử lý việc tracking đối tượng
        frame = Det.draw_all_box(frame, detections, au_altitude, frame_height, frame_width, gimbal_yaw)
            #frame = Det.draw_all_box(frame, detections)

        

        cv2.imshow("Tracking", frame)
        
        key = cv2.waitKey(1)
        if key == 27:  # ESC key
            break
       
    cv2.destroyAllWindows()