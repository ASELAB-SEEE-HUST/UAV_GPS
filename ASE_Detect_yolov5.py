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
import socketserver
import struct
import datetime
import pandas as pd

lat = 0
long = 0
alt = 0
gocpit = 0
gocroll = 0
gocyaw = 0
count1 = 0
count2 = 0
frame_width = 1280
frame_height = 720
HFOV = 82.1
VFOV = 50.76
pi = math.pi
DRT = pi/180
gimbal_yaw = 0

class MyTCPHandler(socketserver.BaseRequestHandler):
    def handle(self):
        print("get....")
        try:
            data_length_bytes = self.request.recv(8)
            data_length = struct.unpack('>Q', data_length_bytes)[0]

            data = bytearray()
            while len(data) < data_length:
                chunk = self.request.recv(1024)
                if not chunk:
                    break
                data.extend(chunk)

            received_data = data.decode("utf-8")
            print("Received data from client:", received_data)

            values = received_data.split(' ')

            
            global lat, long, alt, gocpit, gocroll, gocyaw
            lat = float(values[1])
            long = float(values[0])                                                                                                         
            alt = float(values[2])
            gocpit = float(values[3])
            gocroll = float(values[4])
            gocyaw = float(values[5])
            

            self.set_lat(lat)
            self.set_long(long)
            self.set_alt(alt)
            self.set_gocpit(gocpit)
            self.set_gocroll(gocroll)
            self.set_gocyaw(gocyaw)
                

            if received_data is not None:
                self.send_data("OKE")
           
        except Exception as e:
            print(self.client_address, "Connection disconnected")
        finally:
            self.request.close()

    # Gửi dữ liệu cho client
    def send_data(self, data):
        data_bytes = data.encode("utf-8")
        data_length = len(data_bytes)
        self.request.sendall(data_length.to_bytes(8, byteorder='big'))
        self.request.sendall(data_bytes)

    def setup(self):
        now_time = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        print(now_time)
        print("Connection established:", self.client_address)

    def finish(self):
        print("release connection")

    def set_lat(self, value):
        global lat
        lat = value

    def set_long(self, value):
        global long
        long = value

    def set_alt(self, value):
        global alt
        alt = value

    def set_gocpit(self, value):
        global gocpit
        gocpit = value

    def set_gocroll(self, value):
        global gocroll
        gocroll = value

    def set_gocyaw(self, value):
        global gocyaw
        gocyaw = value

    # Các hàm getter
    def get_lat(self):
        global lat
        return lat

    def get_long(self):
        global long
        return long

    def get_alt(self):
        global alt
        return alt

    def get_gocpit(self):
        global gocpit
        return gocpit

    def get_gocroll(self):
        global gocroll
        return gocroll

    def get_gocyaw(self):
        global gocyaw
        return gocyaw

class Detect:
    def __init__(self, weights, img_size = 640, device ='cpu' ):
        # Initialize
        set_logging()
        self.device = select_device(device)
        print(self.device)
        self.half = self.device.type != 'cpu'  # half precision only supported on CUDA

        # Load model
        self.model = attempt_load(weights, map_location=self.device)  # load FP32 model
        self.stride = int(self.model.stride.max())  # model stride
        self.img_size = check_img_size(img_size, s=self.stride)  # check img_size


        if self.half:
            self.model.half()  # to FP16
        if self.device.type != 'cpu':
            self.model(torch.zeros(1, 3, self.img_size, self.img_size).to(self.device).type_as(next(self.model.parameters())))  # run once
        self.names = self.model.module.names if hasattr(self.model, 'module') else self.model.names
        self.colors = [[random.randint(0, 255) for _ in range(3)] for _ in self.names]
        
        # Initialize tracking variables
        self.tracking_objects = {}
        self.object_positions = {}
        self.object_timestamps = {}
        self.track_id = 0
    
    # def precess_img(self, img, size):
    #     '''Resize image and convert to tensor 4D'''
    #     img = letterbox(img,size)[0]
    #     img_norm = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    #     img_norm = img_norm.astype(np.float16)/255
    #     img_norm = np.transpose(img_norm, (2, 0, 1))   
    #     return torch.from_numpy(img_norm).unsqueeze(0)
    # def detect(self, image, conf_thres=0.25, iou_thres=0.45, classes=None, agnostic_nms=False, augment=False):
    #     img = self.precess_img(image, (640, 640))
    #     self.img_size_detect = img.shape

    #     # Move the model and the input image to the selected device
    #     self.model.to(self.device)
    #     img = img.to(self.device)

    #     pred = self.model(img.half(), augment=augment)[0]  # detect
    #     # Apply NMS
    #     pred = non_max_suppression(pred, conf_thres, iou_thres, classes=classes, agnostic=agnostic_nms)
    #     return pred

    def precess_img(self, img, size):
        '''Resize image and convert to tensor 4D'''
        img = letterbox(img,size)[0]
        img_norm = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        img_norm = img_norm.astype(np.float32)/255
        img_norm = np.transpose(img_norm, (2, 0, 1))   
        return torch.from_numpy(img_norm).unsqueeze(0)
    def detect(self, image, conf_thres = 0.25, iou_thres=0.45, classes=0, agnostic_nms = False, augment =False):
        img = self.precess_img(image,(640,640))
        self.img_size_detect = img.shape
        pred = self.model(img, augment=augment)[0]  #detect
        # Apply NMS
        pred = non_max_suppression(pred, conf_thres, iou_thres, classes=classes, agnostic=agnostic_nms)
        return pred
    
    def get_center(self, box):
        y = box.clone() if isinstance(box, torch.Tensor) else np.copy(box)
        y[:, 0] = (box[:, 0] + box[:, 2]) / 2  # x center
        y[:, 1] = (box[:, 1] + box[:, 3]) / 2  # y center
        return y
    def get_all_center(self, all_box):
        center = torch.tensor([])
        for i in all_box:
            center = np.append(center, self.get_center(i))
        

    def calculate_person_speed(self, x1, y1, x2, y2, au_altitude, frame_height, frame_width, time_diff, gimbal_yaw):
            uav_speed = 0
            uav_speed_x = uav_speed * math.cos(math.radians(gimbal_yaw))
            uav_speed_y = uav_speed * math.sin(math.radians(gimbal_yaw))
            AH = au_altitude*math.tan(math.radians(55))
            # khoảng cách từ UAV đến trung tâm ảnh
            AK = au_altitude*math.tan(math.radians(70))

            ratioY1 = (frame_height - y1) / (frame_height/2)
            ratioY2 = (frame_height - y2) / (frame_height/2)

            distance_UAV_victim1 = (AH + ratioY1 * (AK - AH))/1000
            distance_UAV_victim2 = (AH + ratioY2 * (AK - AH))/1000

            # Vận tốc victim the Ox, Oy
            victim_speed_x = (distance_UAV_victim2 - distance_UAV_victim1) / time_diff
            victim_speed_y = (x2-x1) / (time_diff)

            # Vận tốc thực victim theo hướng Ox,Oy
            victim_speed_real_x = victim_speed_x - uav_speed_x
            victim_speed_real_y = victim_speed_y - uav_speed_y

            victim_speed = math.sqrt(victim_speed_real_x ** 2 + victim_speed_real_y ** 2)

            return victim_speed
    
    
    
    def get_GPS_UAV():

        UAV_latitude = MyTCPHandler.get_lat(None)
        UAV_longitude =  MyTCPHandler.get_long(None)
        UAV_altitude =  MyTCPHandler.get_alt(None)
        return UAV_latitude , UAV_longitude ,UAV_altitude
    
    def calculate_coordinates(latitude_a, longitude_a, distance, bearing):
        # Chuyển đổi độ sang radian
        latitude_a = math.radians(90 - latitude_a)
        longitude_a = math.radians(longitude_a)
        bearing = math.radians(bearing)

        R = 6371000

        # Tính kinh độ và vĩ độ của điểm B
        latitude_b = math.asin(math.sin(latitude_a) * math.cos(distance / R) + math.cos(latitude_a) * math.sin(distance / R) * math.cos(bearing))
        longitude_b = longitude_a + math.atan2(math.sin(bearing) * math.sin(distance / R) * math.cos(latitude_a), math.cos(distance / R) - math.sin(latitude_a) * math.sin(latitude_b))

        # Chuyển đổi radian sang độ
        latitude_b = 90 - math.degrees(latitude_b)
        longitude_b = math.degrees(longitude_b)

        return latitude_b, longitude_b


    def GPS_victim(self, coordinatesX, coordinatesY, lat, long, alt):
        UAV_latitude = lat
        UAV_longitude = long
        UAV_altitude = alt
        frame_width = 1280
        frame_height = 720
        
        HFOV = 82.1
        VFOV = 50.76
        DRT = math.pi / 180

        

        alpha_yaw = 0
        if((coordinatesX > ((frame_width / 2)-1)) & (coordinatesX  < (( frame_width / 2) + 1))):
            alpha_yaw = 0

        elif(coordinatesX  <= ((frame_width / 2)-1)):
            ratioX = ((frame_width/2) - coordinatesX ) / (frame_width / 2)   # tỉ lệ 
            alpha_yaw = math.atan(math.tan((HFOV/2)* DRT) * ratioX)
            alpha_yaw = -alpha_yaw

        elif(coordinatesX  >= ((frame_width / 2)+1)):
            ratioX = (coordinatesX -(frame_width/2)) / (frame_width / 2)
            alpha_yaw = math.atan(math.tan((HFOV/2)* DRT) * ratioX)

        # khoảng cách từ UAV đến mép dưới ảnh
        AB = UAV_altitude*math.tan(math.radians(90 + MyTCPHandler.get_gocpit(None)-VFOV/2))
            # khoảng cách từ UAV đến trung tâm ảnh
        AC = UAV_altitude*math.tan(math.radians(90 + MyTCPHandler.get_gocpit(None)))
            # tỉ lệ khoảng xách victim ở dưới khung hình
        ratioY = (frame_height - coordinatesY) / (frame_height/2)

        
        AV = (AB + ratioY * (AC - AB))

        
        # ase_yaw_cur=client.getDREF(ase_yaw)
        # ase_yaw_data_cur = ase_yaw_cur[0]
            #khoảng cách từ UAV đến victim
            # alpha1 là góc đến giữa 
        # print (AV,alpha_yaw)
        # khoảng các từ UAV đến victim theo phương ox từ UAV nhìn xuống
        SV = math.sqrt(UAV_altitude**2 + AV**2)
        # khoảng cách từ vicim đến hình chiếu của victim
        VO = SV*math.tan(alpha_yaw)
        # print(SV,VO)
        AO= math.sqrt(VO**2 + AV**2)

        yaw_UAV_victim = ((math.atan(VO/AV))/DRT) + MyTCPHandler.get_gocyaw(None)
        # print(AO, yaw_UAV_victim)
        # print(AO,alpha_yaw/DRT,yaw_UAV_victim)
        
        latitude_b, longitude_b = Detect.calculate_coordinates(UAV_latitude, UAV_longitude ,AO ,yaw_UAV_victim)
        return latitude_b, longitude_b


    def draw_all_box(self, img, detections, frame_height, frame_width, gimbal_yaw):
        # Process detections
        for _, det in enumerate(detections):  # detections per image
            s, im0 = '', img
            if len(det):
                # Rescale boxes from img_size to im0 size
                det[:, :4] = scale_coords(self.img_size_detect[2:], det[:, :4], im0.shape).round()
                # Print results
                for c in det[:, -1].unique():
                    n = (det[:, -1] == c).sum()  # detections per class
                    s += f"{n} {self.names[int(c)]}{'s' * (n > 1)}, "  # add to string
                
                # Write results and calculate speed
                for i, (*xyxy, conf, cls) in enumerate(reversed(det)):
                    label = f'{self.names[int(cls)]}'
                    cx, cy = (xyxy[0] + xyxy[2]) / 2, (xyxy[1] + xyxy[3]) / 2
                    x=float(cx)
                    y=float(cy)
                    # print(x,y)
                    # Calculate speed
                    object_id = None
                    for obj_id, obj_pt in self.tracking_objects.items():
                        distance = math.hypot(cx - obj_pt[0], cy - obj_pt[1])
                        if distance <= 50:
                            object_id = obj_id
                            break

                    if object_id is None:
                        object_id = self.track_id
                        self.track_id += 1

                    self.tracking_objects[object_id] = (cx, cy)

                    current_time = time.time()
                    if object_id not in self.object_positions:
                        self.object_positions[object_id] = []
                        self.object_timestamps[object_id] = []
                    self.object_positions[object_id].append((cx, cy))
                    self.object_timestamps[object_id].append(current_time)

                    # Calculate speed if enough data
                    if len(self.object_positions[object_id]) >= 2:
                        prev_position = self.object_positions[object_id][-2]
                        prev_time = self.object_timestamps[object_id][-2]
                        time_diff = current_time - prev_time
                        if time_diff > 0:
                                                                
                            speed = None
                            
                            # speed = self.calculate_person_speed(prev_position[0], prev_position[1], cx, cy,au_altitude, frame_height, frame_width, time_diff, gimbal_yaw)

                            UAV_latitude = MyTCPHandler.get_lat(None)
                            UAV_longtitude = MyTCPHandler.get_long(None)
                            UAV_altitude =  MyTCPHandler.get_alt(None)

                            gps = self.GPS_victim(x, y, UAV_latitude,UAV_longtitude,UAV_altitude)
                            print(gps)
                           
                            
                            im0 = plot_one_box(xyxy, im0, label=label, color=self.colors[int(cls)],line_thickness=1, speed=speed, object_id=object_id, gps=gps)

        return im0
   
        

