import cv2

#rtsp_url = "rtsp://trung:12345678@192.168.0.201:8554/streaming/live/1"
rtsp_url = "rtsp://anhdz:123@192.168.0.18:8554/streaming/live/1"
rtmp_url = "rtmp://a.rtmp.youtube.com/watch?v=wp90u2BtxVI"

rtsp_camera = cv2.VideoCapture(rtsp_url)

while(rtsp_camera.isOpened()):
    # Đọc một khung hình
    ret, frame = rtsp_camera.read()
    
    # Kiểm tra xem khung hình có tồn tại không
    if ret:
        # Hiển thị khung hình
        cv2.imshow('image', frame)
    else:
        print("Không thể đọc được khung hình từ luồng RTSP.")
        break
    
    # Kiểm tra phím bấm để thoát vòng lặp
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

# Giải phóng các tài nguyên
rtsp_camera.release()
cv2.destroyAllWindows()