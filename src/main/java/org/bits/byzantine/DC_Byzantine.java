package org.bits.byzantine;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bits.byzantine.message.AckMessage;
import org.bits.byzantine.message.OrderMessage;

/**
 * Implementation of randomized Byzantine agreement without authentication algorithm (Lamport-Pease-Shostak).
 * Synchronous communication is modeled by sending each message from node to other nodes each round.
 */
public class DC_Byzantine extends UnicastRemoteObject implements DC_Byzantine_RMI, Runnable, Observer {

    private static final long serialVersionUID = 2526720373028386278L;
    private static Log LOGGER = LogFactory.getLog(DC_Byzantine.class);
    private static final int TIME_OUT_MS = 500;

    //Cache to fasten lookup operations in remote registers
    private Map<String, DC_Byzantine_RMI> processCache;

    //Index of a current process
    private int index;

    //Number of processes participating in message exchange
    private int numProcesses;

    //URLs of processes in a system
    private String[] urls;

    //Counter of a messages sent by the process
    private int nextMessageId = 1;

    //list of orders come to a decision
    //private Map<String, Order> orders = new HashMap<String, Order>();

    //final decision
    private Order finalOrder = null;

    //queue of incoming messages needed to simulate synchronous communication
    private Map<List<Integer>, OrderMessage> incomingMessages = new HashMap<List<Integer>, OrderMessage>();

    //queue of outgoing messages needed to simulate synchronous communication
    private List<OrderMessage> outgoingMessages = new LinkedList<OrderMessage>();

    private Map<List<Integer>, OrderSet> orderSets = new HashMap<List<Integer>, OrderSet>();

    private Map<Integer, Step> stepMap = new HashMap<Integer, Step>();

    private long lastOutcomingCheck = 0;

    private boolean firstMessageReceived = false;

    /**
     * Default constructor following RMI conventions
     *
     * @param urls  URLs of participating processes
     * @param index index of current process
     * @throws RemoteException if RMI mechanisms fail
     */
    protected DC_Byzantine(String[] urls, int index) throws RemoteException {
        super();
        processCache = new HashMap<String, DC_Byzantine_RMI>();

        this.index = index;
        this.urls = urls;
        this.numProcesses = urls.length;

        reset();
    }

    public void run() {
        //intentionally left blank
    }

    public boolean isDone() {
        return finalOrder != null;
    }

    public void receiveOrder(OrderMessage message) throws RemoteException {
        LOGGER.debug(echoIndex() + "received order from " + message.getSender() + ", order: " + message.getOrder());
        incomingMessages.put(message.getAlreadyProcessed(), message);
        sendAck(message);

        //valid for the exchange rounds starting from 2, when commander doesn't participate in the exchange process
        if (message.getSender() != 0) {
            LOGGER.debug(echoIndex() + "Traitors: " + message.getMaxTraitors());
            Step step = stepMap.get(message.getMaxTraitors());
            if (step == null) {
                step = new Step(numProcesses, message.getMaxTraitors());
                stepMap.put(message.getMaxTraitors(), step);
            }
            step.addMessage(message);
            if (step.isReady()) {
                LOGGER.debug(echoIndex() + "Step ready");
                processStep(step);
            } else if (step.isWaitingForMissedMessages()) {
                LOGGER.debug(echoIndex() + "Step waiting");
                try {
                    Thread.sleep(Step.WAITING_TIME_OUT * 2);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (firstMessageReceived) {
                    processStep(step);
                }
            }

            //valid for rounds 0 and 1, when commander receives order from client (0)
            // and redistributes it among lieutenants (1)
        } else {
            firstMessageReceived = true;
            process(message);
            Step firstStep = stepMap.get(message.getMaxTraitors() - 1);
            if (firstStep != null && firstStep.isReady()) {
                LOGGER.debug(echoIndex() + "firstStep ready");
                processStep(firstStep);
            }
        }
    }

    private void processStep(Step step) {
        if (step.isReady()) {
            LOGGER.debug(echoIndex() + "Step ready");
            LOGGER.debug(echoIndex() + "Processing step with " + step.getMaxTraitors() + " traitors " +
                    "and " + step.getMessages().size() + " messages");
            for (OrderMessage msg : step.getMessages()) {
                process(msg);
            }
            stepMap.remove(step.getMaxTraitors());
        } else {
            LOGGER.error(echoIndex() + "Cannot process step with " + step.getMaxTraitors() + " traitors: step not ready.");
            throw new RuntimeException();
        }
    }

    /**
     * Apply the Lamport-Pease-Shostak distributed algorithm for solving the Byzantine Generals problem.
     *
     * @param message The message to be processed.
     */
    private void process(OrderMessage message) {
        LOGGER.debug(echoIndex() + "processing " + message.toString() + " from " + message.getSender());

        Order order = message.getOrder();
        Integer maxTraitors = message.getMaxTraitors();
        List<Integer> alreadyProcessed = message.getAlreadyProcessed();

        //LOGGER.debug("***" + echoIndex() + ": Already processed list size - " + alreadyProcessed.size());
        if (alreadyProcessed.isEmpty()) // Initial case of the recursion (commander)
        {
        	LOGGER.debug(echoIndex() + ": Broadcasting order to others...");
            finalOrder = order;
            broadcastOrder(maxTraitors, order, alreadyProcessed);
            return;
        }

        OrderSet dependentOrderSet = null;
        List<Integer> keyPreviousRecursionStep;
        if (alreadyProcessed.size() > 1) {
            keyPreviousRecursionStep = alreadyProcessed.subList(0, alreadyProcessed.size() - 1);
            dependentOrderSet = this.orderSets.get(keyPreviousRecursionStep);
        }

        if (maxTraitors != 0 /*&& null != dependentOrderSet*/) // Intermediate case of the recursion (lieutenant)
        {
            OrderSet orderSet = new OrderSet(maxTraitors, alreadyProcessed, order);
            if (alreadyProcessed.size() > 1) {
                orderSet.addObserver(dependentOrderSet);
            } else // The top of the recursion.
            {
                orderSet.addObserver(this);
            }
            this.orderSets.put(alreadyProcessed, orderSet);

            broadcastOrder(maxTraitors - 1, order, alreadyProcessed);
        } else // Bottom case of the recursion (lieutenant)
        {
            if (alreadyProcessed.size() == 1) // Initial case of the recursion (lieutenant) when f=0.
            {
                this.finalOrder = order;
            } else {
                dependentOrderSet.add(alreadyProcessed, order);
            }
        }
    }

    private void sendAck(OrderMessage orderMessage) {
        AckMessage message = getAckMessageTemplate(orderMessage.getSender());
        message.setAckId(message.getId());
        DC_Byzantine_RMI receiver = getProcess(urls[orderMessage.getSender()]);
        try {
            receiver.receiveAck(message);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void receiveAck(AckMessage message) throws RemoteException {
        OrderMessage messageToRemove = null;
        for (OrderMessage om : outgoingMessages) {
            if (om.getId() == message.getAckId()) {
                messageToRemove = om;
                break;
            }
        }
        outgoingMessages.remove(messageToRemove);

        //second attempt to delivery failed messages, if it also fails we give up
        long now = System.currentTimeMillis();
        List<OrderMessage> messagesToRemove = new LinkedList<OrderMessage>();
        if (now - lastOutcomingCheck > TIME_OUT_MS) {
            lastOutcomingCheck = now;
            for (OrderMessage om : outgoingMessages) {
                if (now - om.getTimestamp() > TIME_OUT_MS) {
                    try {
                        DC_Byzantine_RMI dest = getProcess(urls[om.getReceiver()]);
                        dest.receiveOrder(om);
                        messagesToRemove.add(om);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            outgoingMessages.removeAll(messagesToRemove);
        }
    }

    /**
     * Constructs a template of a new message
     *
     * @param receiver
     * @return
     */
    private OrderMessage getOrderMessageTemplate(int receiver) {
        nextMessageId++;
        return new OrderMessage(nextMessageId - 1, index, receiver);
    }

    /**
     * Constructs a template of a new acknowledgment message
     *
     * @param receiver
     * @return
     */
    private AckMessage getAckMessageTemplate(int receiver) {
        nextMessageId++;
        return new AckMessage(nextMessageId - 1, index, receiver);
    }

    public void reset() {

    }

    /**
     * {@inheritDoc}
     */
    public int getIndex() {
        return index;
    }

    public boolean isFaulty() throws RemoteException {
        // TODO Auto-generated method stub
        return false;
    }

    public void setFaulty(boolean isFaulty) throws RemoteException {
        // TODO Auto-generated method stub

    }

    /**
     * Returns a process specified by its URL either from a local cache or RMI lookup.
     *
     * @param url process url
     * @return process
     */
    private DC_Byzantine_RMI getProcess(String url) {
        DC_Byzantine_RMI result = processCache.get(url);
        if (result == null) {
            try {
                result = (DC_Byzantine_RMI) Naming.lookup(url);
            } catch (RemoteException e1) {
                throw new RuntimeException(e1);
            } catch (MalformedURLException e2) {
                throw new RuntimeException(e2);
            } catch (NotBoundException e3) {
                throw new RuntimeException(e3);
            }
            processCache.put(url, result);
        }
        return result;
    }


    private void broadcastOrder(int maxTraitors, Order order, List<Integer> alreadyProcessed) {
        for (int i = 0; i < numProcesses; i++) {
            if (index != i && !alreadyProcessed.contains(i)) {
                DC_Byzantine_RMI destination = getProcess(urls[i]);

                OrderMessage messageCopy = getOrderMessageTemplate(i);
                messageCopy.setMaxTraitors(maxTraitors);
                messageCopy.setOrder(order);
                messageCopy.setAlreadyProcessed(new LinkedList<Integer>(alreadyProcessed));
                messageCopy.getAlreadyProcessed().add(index);

                try {
                    destination.receiveOrder(messageCopy);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private String echoIndex() {
        return "[" + index + "] ";
    }

    /* (non-Javadoc)
      * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
      */

    /**
     * Update observer with the outcome of the majority vote of the set of the Observable and the corresponding key.
     */
    @SuppressWarnings("unchecked")
    public void update(Observable arg0, Object arg1) {
        //LOGGER.debug("---Update called-------------------------------------------");
        AbstractMap.SimpleEntry<List<Integer>, Order> se;
        if (arg1 instanceof AbstractMap.SimpleEntry<?, ?>) {
            try {
                se = (AbstractMap.SimpleEntry<List<Integer>, Order>) arg1;
                this.finalOrder = se.getValue();
                
                LOGGER.debug(echoIndex() + " - Majority Voting: " + finalOrder);
            } catch (Exception e) {
                LOGGER.debug("invalid cast to SimpleEntry<List<Integer>, Order>.");
            }
        }
    }
}

