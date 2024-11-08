package org.swemas.core.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.swemas.core.Module;
import org.swemas.core.ModuleNotFoundException;
import org.swemas.core.kernel.IKernel;

/**
 * @author Alexey Chernov
 * 
 */
public class SwEventDispatcher extends Module implements IEventDispatchingChannel {

    /**
	 * @param kernel
	 */
    public SwEventDispatcher(IKernel kernel) {
        super(kernel);
        for (int i = 0; i < 100; ++i) _queues.add(new ConcurrentHashMap<Class<?>, List<Event>>());
    }

    @Override
    public void registerQueue(Class<?> eventType) {
        for (int i = 0; i < 100; ++i) _queues.get(i).put(eventType, new CopyOnWriteArrayList<Event>());
        _listeners.put(eventType, new CopyOnWriteArrayList<IEventListeningChannel>());
    }

    @Override
    public void event(Event event) {
        event(event, 50);
    }

    @Override
    public void event(Event event, int priority) {
        Map<Class<?>, List<Event>> qp = _queues.get(priority);
        if (!qp.containsKey(event.getClass())) qp.put(event.getClass(), new CopyOnWriteArrayList<Event>());
        qp.get(event.getClass()).add(event);
        new Thread(new EventRotator()).start();
    }

    @Override
    public void addListener(Class<?> eventType, IEventListeningChannel listener) {
        _listeners.get(eventType).add(listener);
    }

    @Override
    public void removeListener(Class<?> eventType, IEventListeningChannel listener) {
        _listeners.get(eventType).remove(listener);
    }

    private class EventRotator implements Runnable {

        public EventRotator() {
        }

        @Override
        public void run() {
            for (int i = 0; i < 100; ++i) {
                Map<Class<?>, List<Event>> mq = SwEventDispatcher.this._queues.get(i);
                for (Class<?> c : mq.keySet()) {
                    List<IEventListeningChannel> ls = SwEventDispatcher.this._listeners.get(c);
                    List<Event> le = mq.get(c);
                    for (IEventListeningChannel l : ls) for (Event e : le) new Thread(new EventProcessor(e, l)).start();
                }
            }
        }
    }

    private class EventProcessor implements Runnable {

        public EventProcessor(Event event, IEventListeningChannel listener) {
            _event = event;
            _listener = listener;
        }

        @Override
        public void run() {
            try {
                _listener.process(_event);
            } catch (RuntimeException e) {
                try {
                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                    ed.event(new EventDispatchingEvent(_event, _listener, e));
                } catch (ModuleNotFoundException e1) {
                }
            }
        }

        private Event _event;

        private IEventListeningChannel _listener;
    }

    private List<Map<Class<?>, List<Event>>> _queues = new CopyOnWriteArrayList<Map<Class<?>, List<Event>>>();

    private Map<Class<?>, List<IEventListeningChannel>> _listeners = new ConcurrentHashMap<Class<?>, List<IEventListeningChannel>>();
}
