Reflection - библиотека, которая позволяет оперировать информацией о типах во время выполнения.
Пакеты:
* java.lang
* java.lang.reflection

### Информация о типе

Класс `Class<T>` - информация о типе.
Предоставляет информацию о:
* структуре класса
* структура наследования
* проверка времени выполнения


*object.getClass() (во время исполнения) - возвращает информацию о классе, которому реально
принадлежит объект
* Type.class (во время компиляции)

- Можно брать класс даже от примитива и от массивов
`int.class`

* У любого класса есть статическое поле `class` (и даже у примитива):
```
ArrayList.class
int.class
int[].class
```
Его делает JVM, через reflection его не достать.

* У классов оберток есть еще виртуальное поле `Integer.TYPE`, который
возвращает информацию о типе-примитиве. Т.е. `Integer.TYPE == int.class`.

У переменной типа `Class` можно спросить, описанием какого класса она является:
```
isAnnotation
isArray
isPrimitive
isEnum
isInterface
```
У нее можно попросить имя класса (в 3х форматах):
* `getCanonicalName()` - каноническое имя (вместе с пакетом).
* `getName()` - полное имя (если вложенный класс, то с именами всех родителей).
* `getSimpleName()` - простое имя (просто имя класса). Для анонимного класса
вернет $.

Можно спросить структуру класса и модификаторы:
* `getSuperClass()` - предок.
* `getInterface()` - реализуемые итерфейсы.
* `getModifiers()` - модификаторы.

Можно получить место определения класса:

| Тип класса   | Метод                    |
|--------------|--------------------------|
| Верхнего ур. | getPackage()             |
| Вложенный    | getDeclaredClass()       |
|    в констр. | getEnclosingContructor() |
|    в метод   | getEnclosingMethod()     |

Можно проверить приведение типов:
* `isAssignableFrom(class)` - можно ли `class` привести к текущему.
* `isInstance(object)` - является ли объект инстансом класса.
* `cast(object)` - можно ли кастануть объект к классу.
* `if (x insatnceof String) {....}`

###Что бывает внутри класса

Все, что есть, внутри класса - members. Их можно чуть конкретнее разделить на:
* Field
* Methods
* Constructor
* Class (вложенные, внутренние классы и интерфейсы)

**Интерфейс Member**
Методы:
* `getDeclaringClass()`
* `getName()`
* `getModifiers()`

**Модификаторы**

| Константа    | Метод          | Модификатор  |
|--------------|----------------|--------------|
| ABSTRACT     | isAbstract     | abstract     |
| FINAL        | isFinal        | final        |
| INTERFACE    | isInterface    | interface    |
| NATIVE       | isNative       | native       |
| PRIVATE      | isPrivate      | private      |
| PROTECTED    | isProtected    | protected    |
| PUBLIC       | isPublic       | public       |
| STATIC       | isStatic       | static       |
| STRICT       | isStrict       | strict       |
| SYNCHRONIZED | isSynchronized | synchronized |
| TRANSIENT    | isTransient    | transient    |
| VOLATILE     | isVolatile     | volatile     |

**Поля**
Открытые:
* `getFields()` - у класса можно получить список его публичных полей (сюда
же включаются доступные поля из всех его предков)
* `getField(name)` - конкретное поле
* `getDeclaredFields` - список вообще всех полей класса (но без предков)
* `getDeclaredField(name)`

```
System.out.println(Arrays.toString(A.class.getFields()));
\\[public int ex_reflection.A.pub_field]
System.out.println(Arrays.toString(A.class.getDeclaredFields()));
\\[private int ex_reflection.A.priv_field, public int ex_reflection.A.pub_field]
System.out.println(Arrays.toString(A.class.getField("priv_field")); //java.lang.NoSuchFieldException
```

**??** В каком порядке появляются поля в `getFields()`?

**Класс Field**
Это дженерик.

Методы:
* `getName()`
* `getType()` - вернет `Class<?>` с типом, который описывает поле
* `get(object)` - для объектов ссылочного типа. `object` - объект класса,
у которого хотим получить значение
* `getInt(object), getDouble(object), ...` - чтобы дернуть примитив

**Класс Method**

* `getMethods()`
* `getMethod(name, Class ... parameters)`
* `getDeclaredMethods()`
* `getDeclaredMethod(name, Class ... parameters)`
* `getExceptionTypes()` - возможные исключения
* `getReturnType()` - тип возвращаемого значения
* `invoke(Object object, Object ... args)` - вызвать методы с указанными аргументами
Проблемы с вызовом: если у нас есть примитивы в аргументах, то придется
передавать их обертки.

Кидает исключение: `java.lang.reflection.NoSuchMethodException`

**Класс Constructor**

* `getConstructors()`
* `getConstructor(Class... parameters)`
* `getDeclaredConstructors()`
* `getDeclaredConstructor(Class... parameters)`

Кидает исключения: `java.lang.reflection.NoSuchMethodException`

### Доступ к закрытым членам

По умолчанию доступ к закрытым членам запрещен (кинет `IllegalAccessException`)
Но можно сделать так: `setAccessible(boolean)` поменять доступ. НО только
для **одного единственного** экземпляра Field.
* `isAccessible()`

###Немного философии Reflection
* Возможность общаться c JVM, которая имеет доступ куда угодно. Т.е.
таким образом можно достать и поменять что угодно.
* Это все работает медленно (потому что доступ через Field породжает кучу
вызовов всяких функций).
* А что с final полями? Их можно поменять через какой-то "ад, кошмар и ужас",
но тогда все полетит (JIT  не очень предназначен для этого)

###Операции с массивами

**Клевый хак** 
Создаем массив размера 1 нужного типа и берем у него 0 элемент. Это будет
значение по-умолчанию нужного типа.

## Class loader

Позволяет загружать и определять новые классы. Обычно, они грузятся из
файловой системы, но вообще их можно грузить откуда угодно: БД, сеть, архивы,
генерить на лету.

Методы:
* `loadClass(name, resolve?: boolean)` - загружает класс, resolve - загружать
его насильно или нет.
* `findLoadedClass(name)` - ищет уже загруженный класс.
* `resolveClass(class)` - насильно производит статическую инициализацию.

**Дерево загрузчиков**
Они образуют деревья.

* Классы, которые не находятся в родственных отношениях (родитель-ребенок)
могут загрузить разные классы с одним полным именем.