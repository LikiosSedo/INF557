package LAB_08;

import java.util.Map;
import java.util.HashMap;

public class Packet {
	enum Type {
		DV, Other
	}

	public String source;
	public Type t;  // DV only
	private Map<String, Integer> distanceVector; // 存储距离矢量的映射

	// 构造函数
	public Packet(String s) {
		this.source = s;
		this.t = Type.DV;
		this.distanceVector = new HashMap<>();
	}

	// 添加距离记录
	public void addDistance(String destination, int distance) {
		distanceVector.put(destination, distance);
	}

	// 获取距离矢量
	public Map<String, Integer> getDistanceVector() {
		return distanceVector;
	}

	// 获取发送者信息
	public String getSender() {
		return source;
	}

	@Override
	public String toString() {
		if (this.t == Type.DV) {
			StringBuilder sb = new StringBuilder("Distance Vector packet from " + source + ":\n");
			for (Map.Entry<String, Integer> entry : distanceVector.entrySet()) {
				sb.append("  Destination: ").append(entry.getKey()).append(", Distance: ").append(entry.getValue()).append("\n");
			}
			return sb.toString();
		} else {
			return "Unknown packet";
		}
	}

	public void print() {
		System.out.println(this.toString());
	}
}
