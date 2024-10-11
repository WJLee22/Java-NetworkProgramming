import java.io.Serializable;

public class CalcExpr implements Serializable {

	double operand1, operand2;
	char operator;
	
	public CalcExpr(double operand1, char operator, double operand2) {
		this.operand1=operand1;
		this.operator=operator;
		this.operand2=operand2;
	}
	
	
	public double getOperand1() {
		return operand1;
	}

	public double getOperand2() {
		return operand2;
	}

	public char getOperator() {
		return operator;
	}
	
//	@Override
//	public String toString() { // toString 오버라이드하여, 메시지 읽을때 양 사이드에 [] 추가하여 읽음.
//		return "[" + msg + "]";
//	}
}
