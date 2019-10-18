package logic.manager.Managers;

public interface ManagerInterface<T> {
    T getActive();
    void setActive(T newActive);
    void addItem(T item);
    void clear();
}
