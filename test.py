import math
from ASE_Detect_yolov5 import Detect,MyTCPHandler

if __name__ == "__main__":
    latitude_a = 105.84348297640649
    longitude_a = 21.006580273250268
    x = 189.0
    y = 351.5
    distance = 12
    yaw = 0
    gps = Detect.GPS_victim(x, y, latitude_a, longitude_a, distance)
    print(gps)