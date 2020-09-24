`java.util.concurrent` - дополнительные фичи.

* Не все написано на java (много на нативном языке)

#### А зачем?
1. Нет возможности проверить, занята блокировка или нет.
2. `synchronized` отпускает блокировку там, где кончается критическая секция.
Т.е. можем писать критическую секцию только в рамках блока кода. 
3. Нет адресного `notify()`.


#### Почему не можем просто добавить это в `synchronized`?
Реализована на нативном коде. Можно было бы навесить эти штуки, но
тогда `synchronized` был бы медленным.

#### Concurrency utils
Набор классов, которые облегчают работу с:
* блокировками
* атомарными переменными
* примитивами синхронизации
* многопоточными коллекциями
* управление заданиями (????)

# Методы блокировки

## Интерфейс `Lock`
Методы:
* `lock()` - взять блокировку (если не можем взять, то ждем).
* `lockInterruptebly()` - можно кинуть потоку исключение пока он ждет, 
и тогда он вылетит. (в `synchromized`, если поток встал на ожидание,
его никто не мог за уши оттащить оттуда).
* `tryLock(time?)` - пытается захватить блокировку в течении какого-то времени
(если время не указано, то пытаемся захватить, если не можем - уходим).
* `unlock()`
* `newCondition()` - у `Lock` можно порождать новые условия.

## Интерфейс `Condition`:
В противовес `sycnhronized`, где можно было у любого объекта вызвать
`wait()`, есть `Condition`, которые выполняют похожую задачу.   
Методы:
* `await(time?)` - ждать условие (реагирует на прерывания).
* `awaitUntil(deadline)` - ждать до опред. времени (реагирует на
прерывания).
* `awaitUninterruptibly()` - не реагируем на прерывания во время
ожидания.
* `signal()` - аналог `notify()`
* `signalAll()` - аналог `notifyAll()`
Аналогично `wait()`, надо иметь блокировку, чтобы вызывать эти методы.
(то же самое, что для `wait()`: получим `IllegalMonitorStateException`).

Пример про производителя.
```java
void set(Object data) throws InterruptedException {
    lock.lock();
    try {
        while (this.data != null) notFull.await();
        this.data = data;
        notEmpty.signal();
    } finally {
        lock.unlock();
    }
}
```

**ВАЖНО**: всегда надо отпускать блокировку
```kotlin
lock.lock()
try {
    // тут может быть вообще все что угодно
} finally {
    lock.unlock()
}
```

### ReentrantLock
Реализация `Lock`.   
Дополнительные методы:
* `isFair()` - проверяет "честность" блокировки.
`synchronized` всегда нечестный, а локи можно делать честными.
**Зачем????**
* `isLocked()` - проверяет, занята ли блокировка.
* `protected Collection<Thread> getQueuedThreads()` - Returns a collection
 containing threads that **may** be waiting to acquire this lock.
> Что значит "могут ждать?". Это связано с тем, что пока мы несем
 информацию от Lock, то ожидающие потоки могут отвалиться,
 или могут прийти новые, поэтому нельзя обеспечить точную
 информацию.
* `int getQueueLength()` - Returns a collection containing threads
that may be waiting to acquire this lock.
* `boolean hasQueuedThread(thread)` - ждет ли указанный поток
блокировку.
* `boolean hasQueuedThreads()` - есть ли потоки, ждущие блокировку.
* `protected Collection<Thread> getWaitingThreads(condition)` - 
Returns a collection containing those threads that may be waiting
on the given condition associated with this lock.
* `int getWaitQueueLength(condition)`.

### Пример. Bounded Buffer
```java
class BoundedBuffer {
    final Lock lock = new ReentrantLock();
    final Condition notFull = lock.newCondition();
    final Condition notEmpty = lock.newCondition();
    final Object[] items = new Object[100];
    int putptr, takeptr, count;
    
    public void put(Object x) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length)
                notFull.await();
            items[putptr] = x;
            if (++putptr == items.length) putptr = 0;
            ++count;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }
    
    public Object take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0)
            notEmpty.await();
            Object x = items[takeptr];
            if (++takeptr == items.length) takeptr = 0;
            --count;
            notFull.signal();
            return x;
        } finally {
            lock.unlock();
        }
    }
}

```

## Читатели-писатели
* Читателей объекта может быть сколько угодно.
* Писатель один.
* Пока кто-то пишет, читать нельзя.

### Интерфейс `ReadWriteLock`  
Методы:
* `readLock()` - блок для читателей.
Ждем, если кто-то взял `writeLock`.
* `writeLock()` - блок для писателей.
Ждем, если кто-то взял `readLock` или `writeLock`.   
Реализация: `ReentrantReadWriteLock`.  


# Управление заданиями
## Исполнители

Переложить поток на процесс - довольно дорогая задача (надо закинуть
весь набор инструкций, кэши and so on, а потом убрать это).
Много потоков => очень много времени уходит на переключение в планировщике потоков.
Поэтому делают пул потоков.

### Интерфейс `Executor`
* `execute(Runnable)` - выполнить задание. Подходит, если мы просто
хотим выполнить код, а на возвращаемое значение нам плевать.

Возможные варианты выполнения:
* В том же потоке
* В новом потоке
* Пул потоков
* Наращиваемый пул потоков

### Функции и результаты
**Интерфейс `Callable<V>`** - функция
* `V call()` - посчитать функцию.

**Интерфейс `Future<V>`** - результат (контейнер, в котором когда-нибудь, появится
результат выполнения `Callable`)
* `get(timeout?)` - кидает исключение, если время ожидания вышло.
* `isDone()`
* `boolean cancel(boolean mayInterruptIfRunning)` - отменить выполнение
(параметр - if the thread executing this task should be interrupted;
 otherwise, in-progress tasks are allowed to complete).
* `isCanceled()`.
**ВАЖНО**: Хранит значение или (если вылетело исключение, то исключение).

**Интерфейс `ExecutorService`**    
Методы:
* `submit(Runnable)`
* `Future<V> submit(Callable<V>)`
* `List<Future> invokeAll(List<Callable>)` - 
* `Future invokeAny(List<Callable>)` - выполняет все задачи, возвращает
значение какой-то, выполнившейся успешно.
* `void shutdown()` - заканчивается работа всех уже засабмиченых
тасок, новые не принимаются. Потоки внутри умрут, когда исполнится
последнее задание.
* `List<Runnable> shutdownNow()` - насильно закрывает потоки, не 
завершая выполнение задач. Возвращает список тасок, которые так и не
дождались выполнения.
Если после `shutdown` засылать таски, будет исключение.

## ThreadPool
Виды:
* `newCachedThreadPool` - при необходимости создает новые потоки, но
старается переиспользовать уже созданные.
* `newFixedThreadPool(int n)` - создает пул из фиксированного количества
потоков.
* `newScheduledThreadPool(int corePoolSize)` - **????**
* `newSingleThreadPool` - пул из одного потока.
Дает гарантию на то, что задачи исполняются последовательно, не более
одной задачи за раз. НЕ эквивалентно `newFixedThreadPool(1)`, т.к.
тут нет гарантий на то, что не произойдет переконфигурация и не
добавится больше потоков в пул.

## Реализация исполнителей (ThreadPoolExecutor)
* `corePoolSize` - минимальное количество потоков.
* `maxPoolSize` - макс. количество потоков.
* `blockingQueue` - очередь заданий.
* `keepAliveTime` - время жизни потока.
* `threadFactory` - фабрика потоков.  

# Примитивы синхронизации
* Семафор
* Многоразовый барьер
* Защелка
* Рандеву

## Semaphore
Очень похож на обычный мьютекс.
* Хранит количество разрешений на вход.
* Теоретически, один поток может запрашивать больше 1 разрешения 
себе (аналогия с большой машиной и местами на парковке).

Методы:
* `Semaphore(int permits, ?boolean fair)` - создает семафор с `permits`
разрешениями. Можно указать честность блокировки (по-умолчанию,
нечестная).
* `void acquire(?int permits)` - получить разрешение. Ждет, пока не получит
разрешение, или не придет прерываение.
* `void acquireUninterruptibly(?int permits)`
* `boolean tryAcquire(?int permits, ?long timeout, ?TimeUnit unit)`
* `int availablePermits()` - возвращает количество имеющихся разрешений
у семафора.
* `protected Collection<Thread> getQueuedThreads()`
* `int getQueueLength()`
* `boolean isFair()`
* `protected void reducePermits(int reduction)` - сразу сокращает
количество возможных блокировок (т.е. какое-то время работающих
потоков будет меньше, чем)
* `void release(int permits)`

## CyclicBarrier
Когда набралось достаточное количество ожидающих потоков, барьер
падает, и потоки заходят.
* `CyclicBarrier(int parties, ?Runnable barrierAction)` - можно
поставить действие, которое будет выполнять последний зашедший поток.
* `await(?time)`
* `reset()` - восстанавливает барьер
* `isBroken()`

## CountDownLatch
Отличие от барьера: те, кто двигают защелку и те, кто ждут - разные
сущености (но вообще одна сущность может и щеколду подвинуть, и
потом пойти ждать).
* `countDown()` - опускает защелку на один.
* `await()` - ждет спуска защелки.
* `getCount()` - возвращает текущее значение защелки.

# Интересные вещи и то, что может быть на летучке
* Зачем может быть нужна честная блокировка?
Пусть у нас есть сокет, и мы не хотим его делать через `SingleThreadExecutor`.
Тогда надо брать блокировки. Скорее всего хотим, чтобы сообщения
посылались в том порядке, в котором они приходят. Тут нужна честная
блокировка.
* **НАПИСАТЬ СЕМАФОР ЧЕРЕЗ ЛОКИ!!!!**
* **НАПИСАТЬ LOCK через CAS + synchronzed!!!!!!!!!**

