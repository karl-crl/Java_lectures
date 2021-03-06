# JMM

* **JMM** -- некоторый свод правил, описывающий набор гарантий, на который может рассчитывать программист, когда
пишет многопоточный код.

Модели памяти могут быть:
* с сильными гарантиями (хороши для программиста, т.к. удобно писать код)
* со слабыми гарантиями (лучше оптимизируются компилятором/JVM/so on)

* С Java 5 появилась новая, менее строгая модель памяти, которая позволяла хорошо оптимизировать многопоточный код.

## 3 основные сущности

1. **Видимость**
2. **Переупорядочивание** (возможны не все, а только те, которые в рамках одно потока не приводят к изменению результата)
3. **Happens-Before**

## Happens-Before
* Абстракция JMM, которая обозначает следующее отношение между объектами: если операция X happens-before операции Y, то
весь код за Y видит изменения, которые произошли до X, в рамках одного потока.
* Связь Happens-before транзитивна

### Кто связан отношение
1. Захват монитора (начало `synchronized` или `lock`) HB все что после него в этом потоке.
2. Все, что было до конца `synchronized` или `unlock` HB возврата блокировки
3. Возврат монитора и последующий захват другим потоком.

В виде картиночек: 

Поток_1-----|Y|----|lock|------|X|-------|unlock|-------|Z|-->     
Поток_2----------------------------|A|---------------|lock|-------|B|---->

* Нельзя X до lock
* Нельзя X после unlock
* Можно Y после lock
* Можно Z до unlock
* Y и X HB B, если мы знаем, что сначала был захвачен монитор в первом потоке.

Чтение-запись:
1. Любые зависимости по ОДНОЙ переменной в рамках ОДНОГО потока связаны соотношением HB. 
2. В одном потоке операции до записи в `volatile` HB записи в `volatile`.
3. В одном потоке чтение из `volatile` HB того, что после чтения.
4. Во всех потоках запись в `volatile` HB чтения из `volatile`


Поток_1----|A:чтение x|---|B:запись x|---|C:запись x|--->       
Поток_2----|X|---|запись в vol|------------------------->        
Поток_3-------------------------|чтение из vol|----|Y|-->
* Нельзя менять местами A, B, C
* Нельзя менять порядок X-> запись в vol -> чтение из vol -> Y


Еще HB:
1. Статическая инициализация HB любые действия с любыми экземплярами объектов
2. Запись с `final` поля конструктора HB все, что после конструктора. **ВАЖНО** Это правило является исключением
из транзитивности! Т.е. если мы как-то увидели результат работы конструктора, то 100% увидим и все `final` поля.
3. Любая работа с объектом и `finalize()`

```kotlin
class A {
    val s: Int = 10
    var a: Int = 11
}

fun aFun() {
    val a = A()
    if (a != null) {
        print(a.s) // точно увидим 10
        print(a.a) // не факт, что увидим
    }
}
```

И еще немного HB:
1. Запуск потока HB Любой код в потоке
2. Зануление переменных, относящихся к потоку HB любой код в потоке.
3. Код в потоке HB `join()` 
4. Код в потоке HB `isAlive() == false`.
5. `interrupt()` потока HB обнаружения факта остановки


**Публикация объекта** -- когда один поток создает объект и присваивает на него ссылку полю, которое может увидеть
другой поток.

Публикация является **безопасной**, если:
1. Запись объекта в поле HB чтения поля (в любых потоках)
2. У объекта все поля `final` (видны все проиниц. поля). Более того, в другом потоке **будут видны все значения,
достижимые из `final` полей**.

## Мифы про JMM
1. **Компьютер делает ровно то, что мы его попросим.** На самом деле нет. Он может как угодно переставлять команды
(в пределах правил), выкидывать часть команд.
2.  **Барьеры - действительно барьеры.** 
3. **Мышление категориями, локальный кэш и глобальная память**.

Проблема:

```java
@JCStressTest
@State
class IRIW {
    int x;
    int y;
    @Actor
    void writer1() {
        x = 1;
    }
    @Actor
    void writer2() {
        y = 1;
    }
    @Actor
    void reader1(IntResult4 r) {
        r.r1 = x;
        r.r2 = y;
    }
    @Actor
    void reader2(IntResult4 r) {
        r.r3 = y;
        r.r4 = x;
    }
}

``` 
* Тут можно легко получить 1010
* Если сделать x, y `volatile`, то уже не получим 1010.

## `synchronized` vs `volatile`

**`volatile` не панацея**

```java
@JCStressTest
@State
public class VolatileCounters {
    volatile int x;
    @Actor
    void actor1() {
        for (int i = 0; i < 10; i++) {
            x++;
        }
    }
    @Actor
    void actor2() {
        for (int i = 0; i < 10; i++) {
            x++;
        }
    }
    @Arbiter
    public void arbiter(IntResult1 r) {
        r.r1 = x;
    }
}
``` 

## Так называемая полусинхронизация (то, что нельзя называть)

```java
class Box {
    int x;
    public Box(int v) {
        x = v;
    }
}
class RacyBoxy {
    Box box;
    public synchronized void set(Box v) {
        box = v;
    }
    public Box get() {
        return box;
    }
}
```

Почему это работает:
* set синхронизованный, он не выполнится
* `get` связан с `set` в одном потоке соотношением HB, поэтому  будет видеть все изменения.
* `get` из другого потока не факт, что сразу увидит изменения, но он не знает, он не видит значение, т.к. еще никто
`set` не выполнил, или еще никто 
