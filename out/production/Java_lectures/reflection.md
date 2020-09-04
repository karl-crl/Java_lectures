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
System.out.println(Arrays.toString(A.class.getField("priv_field")); //java.lang.NoSuchFieldException
```

