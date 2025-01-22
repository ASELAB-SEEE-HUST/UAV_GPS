import math

def haversine(lat1, lon1, lat2, lon2):
    """
    Tính khoảng cách giữa hai điểm GPS trong đơn vị mét.
    
    Tham số:
    lat1, lon1: Vĩ độ và kinh độ của điểm thứ nhất (đơn vị độ).
    lat2, lon2: Vĩ độ và kinh độ của điểm thứ hai (đơn vị độ).
    
    Trả về:
    Khoảng cách giữa hai điểm (đơn vị mét).
    """
    # Đổi đơn vị độ sang radian
    lat1 = math.radians(lat1)
    lon1 = math.radians(lon1)
    lat2 = math.radians(lat2)
    lon2 = math.radians(lon2)
    
    # Kích thước của địa cầu
    R = 6371.0  # Đơn vị: kilômét
    
    # Độ chênh lệch giữa các vĩ độ và kinh độ
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    
    # Áp dụng công thức haversine
    a = math.sin(dlat / 2)**2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon / 2)**2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    distance = R * c
    
    return distance * 1000  # Chuyển đổi thành mét

# Tọa độ của điểm thứ nhất
lat1 = float(input("Nhập vĩ độ của điểm thứ nhất: "))
lon1 = float(input("Nhập kinh độ của điểm thứ nhất: "))

# Tọa độ của điểm thứ hai
lat2 = float(input("Nhập vĩ độ của điểm thứ hai: "))
lon2 = float(input("Nhập kinh độ của điểm thứ hai: "))

# Tính khoảng cách
distance = haversine(lat1, lon1, lat2, lon2)
print("Khoảng cách giữa hai điểm là:", distance, "mét")
