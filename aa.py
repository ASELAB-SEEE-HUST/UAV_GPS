import math

def radian_to_degree(radian):
    degree = radian * (180 / math.pi)
    return degree

def main():
    radian = float(input("Nhập giá trị radian: "))
    degree = radian_to_degree(radian)
    print("Độ:", degree)

if __name__ == "__main__":
    main()
