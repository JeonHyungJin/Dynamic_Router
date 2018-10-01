package Router;

public abstract class BaseLayer {	//Layer들이 상속받는 Class로 Layer의 기본적인 성질들을 가짐
	String layerName;	//Layer이름
	Object upperLayer;	//해당 Layer의 상위 계층
	Object underLayer;	//해당 Layer의 하위 계층

	public BaseLayer(String layerName) {	//초기화
		this.layerName = layerName;
	}

	void setUpperLayer(Object upperLayer) {	//초기화
		this.upperLayer = upperLayer;
	}

	void setUnderLayer(Object underLayer) {	//초기화
		this.underLayer = underLayer;
	}

	Object getUpperLayer() {	//상위 계층 반환해주는 함수
		if ((Object) upperLayer == null) {	//상위 계층이 null인경우
			System.out.println("[Object-getUnderLayer] There is no UnderLayer");
			return null;
		}

		return upperLayer;	//상위 계층 반환
	}

	Object getUnderLayer() {	//하위 계층을 반환해주는 함수
		if ((Object) underLayer == null) {	//하위 계층이 null인 경우
			System.out.println("[Object-getUnderLayer] There is no UnderLayer");
			return null;
		}

		return underLayer;	//하위 계층 반환
	}

	String getLayerName() {	//해당 Layer의 이름 반환
		return layerName;
	}
}