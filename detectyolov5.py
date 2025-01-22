from numpy import random
import numpy as np
from models.experimental import attempt_load
from utils.datasets import letterbox
from utils.general import check_img_size, non_max_suppression, scale_coords, set_logging
from utils.plots import plot_one_box
from utils.torch_utils import select_device
import cv2
import torch
import math
import time

def detect_video(video_path, weights_path, conf_thres=0.25, iou_thres=0.45, max_det=1000, device='cuda:0'):

    device = select_device(device)

    model = torch.load(weights_path, map_location=device)
    model.to(device).eval()


    cap = cv2.VideoCapture(video_path)
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    fps = cap.get(cv2.CAP_PROP_FPS)

    cv2.namedWindow('Yolov5 detect', cv2.WINDOW_NORMAL)
    cv2.resizeWindow('Yolov5 detect', width, height)

    colors = [random.randint(0, 255) for _ in range(3)]
    tracking_objects = {}
    object_positions = {}
    object_timestamps = {}
    track_id = 0
    while True:
        ret, frame = cap.read()
        if not ret:
            break

        img = torch.from_numpy(frame).to(device)

        results = model(img)

        for result in results.xyxy[0]:
            *xyxy, conf, cls = result
            if conf > conf_thres:
                label = f'{model.names[int(cls)]} {conf:.2f}'
                cx, cy = (xyxy[0] + xyxy[2]) / 2, (xyxy[1] + xyxy[3]) / 2
                
                object_id = None
                for obj_id, obj_pt in tracking_objects.items():
                    distance = math.hypot(cx - obj_pt[0], cy - obj_pt[1])
                    if distance <= 50:
                        object_id = obj_id
                        break

                if object_id is None:
                    object_id = track_id
                    track_id += 1

                tracking_objects[object_id] = (cx, cy)

                current_time = time.time()
                if object_id not in object_positions:
                    object_positions[object_id] = []
                    object_timestamps[object_id] = []
                object_positions[object_id].append((cx, cy))
                object_timestamps[object_id].append(current_time)

                if len(object_positions[object_id]) >= 2:
                    prev_position = object_positions[object_id][-2]
                    prev_time = object_timestamps[object_id][-2]
                    time_diff = current_time - prev_time
                    if time_diff > 0:
                        speed = None
                        
                        frame = plot_one_box(xyxy, frame, label=label, color=colors[int(cls)],
                                                line_thickness=1, speed=speed, object_id=object_id)



        cv2.imshow('Detect', frame)
        key = cv2.waitKey(1)
        if key == 27: 
            break

    cap.release()
    cv2.destroyAllWindows()

def main():
    video_path = 'test.mp4'
    weights_path = 'best.pt'
    conf_thres = 0.25
    iou_thres = 0.45
    max_det = 100
    device = 'cuda:0'  
    detect_video(video_path, weights_path, conf_thres, iou_thres, max_det, device)

if __name__ == '__main__':
    main()
