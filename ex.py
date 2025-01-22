import time
import cv2
import threading
import socketserver
from ASE_Detect_yolov5 import Detect,MyTCPHandler
import threading

count1=0
count2=0
gimbal_yaw = 0
    
TFrame = None
lock = threading.Lock()


def handlerFrame(frame):
    global TFrame
    TFrame = frame


def getFrame():
    return TFrame


class StreamThread(threading.Thread):
    def __init__(self, video_path, interval, lock):
        super().__init__()
        self.video_path = video_path
        self.cap = cv2.VideoCapture(video_path)
        self.latest_frame = None
        self.lock = lock
        self.interval = interval
    
    def run(self):
        while True:
            ret, frame = self.cap.read()
            if not ret:
                print("Error: Failed to read frame from video")
                break
            self.lock.acquire()
            handlerFrame(frame)
            self.lock.release()
            global count1
            count1 +=1
            time.sleep(self.interval)

class DetectThread(threading.Thread):
    def __init__(self, detect_interval, lock):
        super().__init__()
        self.detect_interval = detect_interval
        self.Det = Detect("yolov5s.pt", 640, 'cpu')
        self.lock = lock

    def run(self):
        while True:
            self.lock.acquire()
            frame = getFrame()
            self.lock.release()
            if frame is not None:
                start_time = time.time()
                UAV_altitude =  MyTCPHandler.get_alt(None)
                UAV_long =  MyTCPHandler.get_long(None)
                UAV_lat =  MyTCPHandler.get_lat(None)
                detections = self.Det.detect(frame, conf_thres=0.25, iou_thres=0.45)

                detect_time = time.time()
                detect_duration = detect_time - start_time

                frame = self.Det.draw_all_box(frame, detections,  frame_height, frame_width, gimbal_yaw)
                gps_time = time.time()
                draw_duration = gps_time - detect_time
                total_duration = gps_time - start_time
                # print(f" Detect Time: {detect_duration:.4f}s, Draw Time: {draw_duration:.4f}s, Total Time: {total_duration:.4f}s")

                cv2.imshow("Detect", frame)
                cv2.waitKey(1)
                # print(count2)
            time.sleep(self.detect_interval)


    
def server_function():
    HOST = ""
    PORT = 8888
    print("This is host: " + HOST)
    server = socketserver.ThreadingTCPServer((HOST, PORT), MyTCPHandler)
    server.serve_forever()
    

def other_functions():
    gps = Detect.get_GPS_UAV()
    print(gps)


if __name__ == "__main__":
   
    frame_height = 720
    frame_width = 1280


    server_thread = threading.Thread(target=server_function)
    server_thread.start()

    lock = threading.Lock()
    
    # Khởi tạo luồng stream
    stream_thread = StreamThread("C:/Users/hovie/Downloads/vehicle.mp4",0.0219, lock)
    # rtsp://anhdz:123@192.168.0.201:8554/streaming/live/1
    stream_thread.start()

    # Khởi tạo luồng detect
    detect_thread = DetectThread(0.0219, lock)
    detect_thread.start()


    # Vòng lặp chính
    while True:
        key = cv2.waitKey(1)
        if key != 27:  # ESC key
            break
    cv2.destroyAllWindows()

   
    try:
        while True:
            time.sleep(1)  
    except KeyboardInterrupt:
        print("Shutting down the program...")     
        server.shutdown()
        server.server_close()