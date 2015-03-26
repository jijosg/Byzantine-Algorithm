package org.bits.byzantine.message;

import java.util.LinkedList;
import java.util.List;

import org.bits.byzantine.Order;

public class OrderMessage extends Message {
	private static final long serialVersionUID = 1237848060493805748L;
	private Order order;
	int maxTraitors;
	List<Integer> alreadyProcessed;
	
	public OrderMessage(int id, int sender, int receiver){
		super(id, sender, receiver);
		alreadyProcessed = new LinkedList<Integer>();
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public int getMaxTraitors() {
		return maxTraitors;
	}

	public void setMaxTraitors(int maxTraitors) {
		this.maxTraitors = maxTraitors;
	}

	public List<Integer> getAlreadyProcessed() {
		return alreadyProcessed;
	}

	public void setAlreadyProcessed(List<Integer> alreadyProcessed) {
		this.alreadyProcessed = alreadyProcessed;
	}
	
	@Override
	public String toString() {
		return "OrderMessage [order=" + order + ", maxTraitors=" + maxTraitors
				+ ", alreadyProcessed=" + alreadyProcessed + "]";
	}
}
