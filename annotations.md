# Аннотации
**Содержание**:
1. Об аннотациях
2. Annotation processors

## Введение в аннотации
Информация взята из: https://docs.oracle.com/javase/tutorial/java/annotations/index.html
### Что это?
Аннотация - это часть метаданных. Это не часть программы, а какая-то 
информация о программе. Аннотации не могут напрямую влиять на код.

**Use cases**:
* **Информация для компилятора**: например, чтобы помочь найти ошибки,
или подавить варнинги.
* **Compile-time and deployment-time processing**: генерим код или какие-то
файлики (например, XML)
* **Runtime processing**: ?????

## Annotations Basics
### Формат аннотаций

1. В самой простой форме: `@Entity`
2. Аннотации могут включать элементы.
    1. Они могут быть именованные и неименованные.
    2. Могут иметь или не иметь значение.
    3. Если у аннотации только один элемент, то его имя м.б. опущено
```java
@SuppressWarnings(value = "unchecked")
void myMethod() { ... }

@SuppressWarnings("unchecked")
void myMethod() { ... }
```
3. Можно использовать несколько аннотаций на одно и то же объявление.
4. С Java 8 поддерживаются Repeated annotation (когда аннотация с одним
именем навешивается на одно объявление).

### Где можно использовать аннотации?
1. Объявления (классов, полей, методов и др. элементов программы)
2. (С Java 8) аннотации применяются для определения возможностей
использования.
    1. Создание экземпляра класса (???) (`new @Interned MyObject();`)
   2. Каст (`myString = (@NonNull String) str;`)
   3. Ограничение имплементации (`class UnmodifiableList<T> implements
                                          @Readonly List<@Readonly T> { ... }
`)
   4. Объявление исключения (`void monitorTemperature() throws
                                      @Critical TemperatureException { ... }`)
                                    
# Annotation processing

Информация взята со слайдов практики и ресурса (http://hannesdorfmann.com/annotation-processing/annotationprocessing101)

## Основы
* Annotation Preprocessing (далее AP) происходит во время компилляции.
* AP - это встроенный инструмент javac для сканирования и обработки
аннотаций во время компилляции.
* С помощью AP можно регистировать собственные аннотации.
* AP доступен с Java 5.

**Что делает AP?** Если по-простому, то берет java код (или уже
скомпиленый байткод) и генерит выход в виде файлов (обычно это .java
файлы).

**Важно**: AP **НЕ** добавляет код в уже существующие файлы.

## `AbstractProcessor`

Чтобы создать свой AP, нужно отнаследовать его от `AbstractProcessor`.
```java
package com.example;

public class MyProcessor extends AbstractProcessor {

	@Override
	public synchronized void init(ProcessingEnvironment env){ }

	@Override
	public boolean process(Set<? extends TypeElement> annoations, RoundEnvironment env) { }

	@Override
	public Set<String> getSupportedAnnotationTypes() { }

	@Override
	public SourceVersion getSupportedSourceVersion() { }

}
```

Коротко об методах:
* `init(ProcessingEnvironment env)` - у любого AP должен быть **пустой
конструктор**. Но есть метод `init()`, который вызывается у AP.
`Processing env` дает всякую полезную информацию о коде (`Elements`,
`Types`, `Filer`).
* `process(Set<? extends TypeElement> annotations, RoundEnvironment env)` -
типа `main()` для AP. `RoundEnviroment` дает информацию о том, кто
проаннотирован той или иной аннотацией.
* `getSupportedAnnotationTypes()` - здесь необходимо перечислить аннотации,
на которые «подписывается» наш процессор.
* `getSupportedSourceVersion()` - Используется для определения
поддерживаемой версии Java. Обычно возвращается
`SourceVersion.latestSupported()`.Хотя можно возвращать, например,
`SourceVersion.RELEASE_6`.

C Java 7 можно не писать последние 2 метода руками, а генерить их с
помощью аннотаций (больше аннотаций богу аннотаций!):
```java
@SupportedSourceVersion(SourceVersion.latestSupported())
@SupportedAnnotationTypes({
   // Set of full qullified annotation type names
 })
public class MyProcessor extends AbstractProcessor {

	@Override
	public synchronized void init(ProcessingEnvironment env){ }

	@Override
	public boolean process(Set<? extends TypeElement> annoations, RoundEnvironment env) { }
}
```

## Регистрация AP в javac.
* javac запускает JVM для запуска AP => В AP можно использовать какие
угодно классы!
* Для того, чтобы рассказать javac про AP, нужно собрать ему .jar файл.
```
MyProcessor.jar
	- com
		- example
			- MyProcessor.class

	- META-INF
		- services
			- javax.annotation.processing.Processor
```
* В `javax.annotation.processing.Processor` сидя все full qualified
имена процессоров:
```
com.example.MyProcessor
com.foo.OtherProcessor
net.blabla.SpecialProcessor
```

## Пример с паттерном Factory

Пусть есть следующий код:
```java
public class PizzaStore {

  private MealFactory factory = new MealFactory();

  public Meal order(String mealName) {
    return factory.create(mealName);
  }

  public static void main(String[] args) throws IOException {
    PizzaStore pizzaStore = new PizzaStore();
    Meal meal = pizzaStore.order(readConsole());
    System.out.println("Bill: $" + meal.getPrice());
  }
}

public class MealFactory {

  public Meal create(String id) {
    if (id == null) {
      throw new IllegalArgumentException("id is null!");
    }
    if ("Calzone".equals(id)) {
      return new CalzonePizza();
    }

    if ("Tiramisu".equals(id)) {
      return new Tiramisu();
    }

    if ("Margherita".equals(id)) {
      return new MargheritaPizza();
    }

    throw new IllegalArgumentException("Unknown id = " + id);
  }
}
```
Мы ленивые программисты, которые не хотят писать `MealFactory`, а хотим
генерить его с помощью AP.

`@Factory` аннотация:

```java
@Target(ElementType.TYPE) @Retention(RetentionPolicy.CLASS)
public @interface Factory {

  /**
   * The name of the factory
   */
  Class type();

  /**
   * The identifier for determining which item should be instantiated
   */
  String id();
}
```

В чем идея: мы хотим аннотировать классы, которые принадлежат нужной
фабрике (`type()` определяет фабрику), а имя класса будет выглядеть,
как `$id()$something`.

Пример:
```java
@Factory(
    id = "Calzone",
    type = Meal.class
)
public class CalzonePizza implements Meal {

  @Override public float getPrice() {
    return 8.5f;
  }
}
```

**ВАЖНО**: Аннотации **не наследуются**. Т.е. если мы проаннотируем
интерфейс `Meal`, то мы не получим автоматическую аннотацию для всех
его наследников. Поэтому надо аннотировать каждый класс еды ручками.

### Договоренности по поводу аннотаций.

Для каждой аннотации есть **_договоренности_**, которые пользователь
должен соблюдать, если хочет, чтобы его код компилился.

В примере договоренности будут следующие:
1. Только классы (не интерфейсы и не абстрактные классы)могут быть
проаннотированы `@Factory`.
2. Классы проаннотированные этой аннотацией должны иметь публичный
конструктор без параметров (иначе мы не сможем их создавать).
3. Должна быть возможность привести аннотированные классы к «типу»
указанному в параметре аннотации.
4. Классы проаннотированные `@Factory` с одинаковым `type` будут
собраны в единый класс-фабрику. Например, для `type Meal` будет
создан класс `MealFactory`.
5. `Id` – это строка. Она должна быть уникальна для классов с
одинаковым `type`.

### Реализация
#### Шаг 1.
* Добавили методы `getSupportedAnnotationTypes()` и
`getSupportedSourceVersion()`
* `@AutoService(Processor.class)` автоматически сгенерирует
манифест-файл.
```java
@AutoService(Processor.class)
public class FactoryProcessor extends AbstractProcessor {

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    ...
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    Set<String> annotataions = new LinkedHashSet<String>();
    annotataions.add(Factory.class.getCanonicalName());
    return annotataions;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
	...
  }
}
```

#### Шаг 2.
* Для AP очень важны штуки `typeUtils, elementUtils, filter` и `messager`.
Их можно достать из `pricessingEnv`, что мы и делаем в `init()`.
* Чаще всего, в `init()` больше ничего делать не надо.

В `init` получаем ссылки на:
* `Elements` - утилитарный класс для работы с классами `Element`.
* `Type` - утилитарный класс для работы с `TypeMirror`. Соотносит
абстрактное синтаксическое дерево с более подробной инфой о классах.
* `Filter` - класс, чтобы удобно создавать файлы.
* `Messenger` - позволяет репортить ошибки.
```java
@AutoService(Processor.class)
public class FactoryProcessor extends AbstractProcessor {

  private Types typeUtils;
  private Elements elementUtils;
  private Filer filer;
  private Messager messager;
  private Map<String, FactoryGroupedClasses> factoryClasses = new LinkedHashMap<String, FactoryGroupedClasses>();

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    typeUtils = processingEnv.getTypeUtils();
    elementUtils = processingEnv.getElementUtils();
    filer = processingEnv.getFiler();
    messager = processingEnv.getMessager();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    // complete
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    // complete
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
	...
  }
}
```

#### Что такое `Element`
Представление абстрактного синтетического дерева для кода, который мы
компилируем. Дает доступ к исходному коду.

* AP сканирует исходный код. Каждая часть исходного кода представляет
из себя некоторый `Element`.
```java
package com.example;	// PackageElement

public class Foo {		// TypeElement

	private int a;		// VariableElement
	private Foo other; 	// VariableElement

	public Foo () {} 	// ExecuteableElement

	public void setA ( 	// ExecuteableElement
	                 int newA	// TypeElement
	                 ) {}
}
```

* Можно итерироваться по детям:
```java
TypeElement fooClass = ... ;
for (Element e : fooClass.getEnclosedElements()){ // iterate over children
	Element parent = e.getEnclosingElement();  // parent == fooClass
}
```

* Чтобы получить `TypeMirror` у некого `Element` нужно вызвать:
```java
Element element = ...;
element.asType();
```

### Шаг 3. Поиска классов, аннотированых с `@Factory`

```java
@AutoService(Processor.class)
public class FactoryProcessor extends AbstractProcessor {

  private Types typeUtils;
  private Elements elementUtils;
  private Filer filer;
  private Messager messager;
  private Map<String, FactoryGroupedClasses> factoryClasses = new LinkedHashMap<String, FactoryGroupedClasses>();
	...

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    // Itearate over all @Factory annotated elements
    for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Factory.class)) {
  		...
    }
  }
}
```
### Шаг 4. Убираем все, что не является классом

* Проверяем, что аннотированный объект является классом. Если это
не так, то кидаем ошибку.
```java
public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Factory.class)) {

      // Check if a class has been annotated with @Factory
      if (annotatedElement.getKind() != ElementKind.CLASS) {
        error(annotatedElement, "Only classes can be annotated with @%s",
            Factory.class.getSimpleName());
        return true; // Exit processing
      }

      ...
    }


private void error(Element e, String msg, Object... args) {
    messager.printMessage(
    	Diagnostic.Kind.ERROR,
    	String.format(msg, args),
    	e);
  }

 }
```
**ВАЖНО**: После вызова `error()` важно завершить функцию c `return 
true`, чтобы не было краша AP.
> Да, в случае ошибок нужно делать return true, т.к., потенциально,
> перед репортингом ошибки могли уже сгенерить какой-то код, и его
> компилятору тоже нужно будет обработать. Выходить из process можно
> сразу при обнаружении ошибки, а можно отрепортить как можно больше,
> и только после этого закончить работу (чтобы пользователь увидел 
> все проблемы, а не только одну). Также стоит заметить, что если 
> ошибки уровня Warning, а не Error, то завершать процессинг
> не требуется.

> Замечание: Мы знаем, что все классы - `TypeElements`. Почему бы
не проверить `if (! (annotatedElement instanceof TypeElement) )`?
Так не пойдет, поскольку интерфейсы тоже `TypeElements`.

### Шаг 5. Генерация классов.

Для того, чтобы генерить классы, нам понадобятся `FactoryAnnotatedClass` 
(класс, описывающий один проаннотированный класс),
`FactiryGroupedClasses` (класс, описывающий набор проаннотированных
 классов, объединенных одним `type`).
 
Как выглядит класс:
```java
public class FactoryAnnotatedClass {

  private TypeElement annotatedClassElement;
  private String qualifiedSuperClassName;
  private String simpleTypeName;
  private String id;

  public FactoryAnnotatedClass(TypeElement classElement) throws IllegalArgumentException {
    this.annotatedClassElement = classElement;
    Factory annotation = classElement.getAnnotation(Factory.class);
    id = annotation.id();

    if (StringUtils.isEmpty(id)) {
      throw new IllegalArgumentException(
          String.format("id() in @%s for class %s is null or empty! that's not allowed",
              Factory.class.getSimpleName(), classElement.getQualifiedName().toString()));
    }

    // Get the full QualifiedTypeName
    try {
      Class<?> clazz = annotation.type();
      qualifiedSuperClassName = clazz.getCanonicalName();
      simpleTypeName = clazz.getSimpleName();
    } catch (MirroredTypeException mte) {
      DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
      TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
      qualifiedSuperClassName = classTypeElement.getQualifiedName().toString();
      simpleTypeName = classTypeElement.getSimpleName().toString();
    }
  }

  /**
   * Get the id as specified in {@link Factory#id()}.
   * return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Get the full qualified name of the type specified in  {@link Factory#type()}.
   *
   * @return qualified name
   */
  public String getQualifiedFactoryGroupName() {
    return qualifiedSuperClassName;
  }


  /**
   * Get the simple name of the type specified in  {@link Factory#type()}.
   *
   * @return qualified name
   */
  public String getSimpleFactoryGroupName() {
    return simpleTypeName;
  }

  /**
   * The original element that was annotated with @Factory
   */
  public TypeElement getTypeElement() {
    return annotatedClassElement;
  }
}
```

Важные части:
* Кидаем ошибку, потому что 1) мы можем использовать все то же самое,
что и в обычном java-коде 2) так нам не надо передавать `messenger`,
а только поймать в AP исключение и обработать его.
```java
Factory annotation = classElement.getAnnotation(Factory.class);
id = annotation.id(); // Read the id value (like "Calzone" or "Tiramisu")

if (StringUtils.isEmpty(id)) {
    throw new IllegalArgumentException(
          String.format("id() in @%s for class %s is null or empty! that's not allowed",
              Factory.class.getSimpleName(), classElement.getQualifiedName().toString()));
    }
```
* Почему оборачиваем в `try-catch`? Поскольку процессинг происходит до
компиляции, то есть 2 случая:
1. Кто-то уже успел поработать с классом, и есть файл `.class`, откуда
мы можем спокойно дернуть объект типа `Class`.
2. Класс еще не скомпиллирован, получим `MirroredTypeException`.
Лайфхак: `MirroredTypeException` содержит `TypeMirror` от еще нескомп.
класса. Т.е. можно кастануть это к `DeclaredType` и получить доступ к
`TypeElement`, чтобы прочитать полное имя.
```java
try {
  Class<?> clazz = annotation.type();
  qualifiedGroupClassName = clazz.getCanonicalName();
  simpleFactoryGroupName = clazz.getSimpleName();
} catch (MirroredTypeException mte) {
  DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
  TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
  qualifiedGroupClassName = classTypeElement.getQualifiedName().toString();
  simpleFactoryGroupName = classTypeElement.getSimpleName().toString();
}
```


Второй класс:
```java
public class FactoryGroupedClasses {

  private String qualifiedClassName;

  private Map<String, FactoryAnnotatedClass> itemsMap =
      new LinkedHashMap<String, FactoryAnnotatedClass>();

  public FactoryGroupedClasses(String qualifiedClassName) {
    this.qualifiedClassName = qualifiedClassName;
  }

  public void add(FactoryAnnotatedClass toInsert) throws IdAlreadyUsedException {

    FactoryAnnotatedClass existing = itemsMap.get(toInsert.getId());
    if (existing != null) {
      throw new IdAlreadyUsedException(existing);
    }

    itemsMap.put(toInsert.getId(), toInsert);
  }

  public void generateCode(Elements elementUtils, Filer filer) throws IOException {
	...
  }
}
```

### Шаг 6. Генерация классов (все еще)

```java
public class FactoryProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Factory.class)) {

      ...

      // We can cast it, because we know that it of ElementKind.CLASS
      TypeElement typeElement = (TypeElement) annotatedElement;

      try {
        FactoryAnnotatedClass annotatedClass =
            new FactoryAnnotatedClass(typeElement); // throws IllegalArgumentException

        if (!isValidClass(annotatedClass)) {
          return true; // Error message printed, exit processing
         }
       } catch (IllegalArgumentException e) {
        // @Factory.id() is empty
        error(typeElement, e.getMessage());
        return true;
       }

   	   ...
   }


 private boolean isValidClass(FactoryAnnotatedClass item) {

    // Cast to TypeElement, has more type specific methods
    TypeElement classElement = item.getTypeElement();

    if (!classElement.getModifiers().contains(Modifier.PUBLIC)) {
      error(classElement, "The class %s is not public.",
          classElement.getQualifiedName().toString());
      return false;
    }

    // Check if it's an abstract class
    if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
      error(classElement, "The class %s is abstract. You can't annotate abstract classes with @%",
          classElement.getQualifiedName().toString(), Factory.class.getSimpleName());
      return false;
    }

    // Check inheritance: Class must be childclass as specified in @Factory.type();
    TypeElement superClassElement =
        elementUtils.getTypeElement(item.getQualifiedFactoryGroupName());
    if (superClassElement.getKind() == ElementKind.INTERFACE) {
      // Check interface implemented
      if (!classElement.getInterfaces().contains(superClassElement.asType())) {
        error(classElement, "The class %s annotated with @%s must implement the interface %s",
            classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
            item.getQualifiedFactoryGroupName());
        return false;
      }
    } else {
      // Check subclassing
      TypeElement currentClass = classElement;
      while (true) {
        TypeMirror superClassType = currentClass.getSuperclass();

        if (superClassType.getKind() == TypeKind.NONE) {
          // Basis class (java.lang.Object) reached, so exit
          error(classElement, "The class %s annotated with @%s must inherit from %s",
              classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
              item.getQualifiedFactoryGroupName());
          return false;
        }

        if (superClassType.toString().equals(item.getQualifiedFactoryGroupName())) {
          // Required super class found
          break;
        }

        // Moving up in inheritance tree
        currentClass = (TypeElement) typeUtils.asElement(superClassType);
      }
    }

    // Check if an empty public constructor is given
    for (Element enclosed : classElement.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
        ExecutableElement constructorElement = (ExecutableElement) enclosed;
        if (constructorElement.getParameters().size() == 0 && constructorElement.getModifiers()
            .contains(Modifier.PUBLIC)) {
          // Found an empty constructor
          return true;
        }
      }
    }

    // No empty constructor found
    error(classElement, "The class %s must provide an public empty default constructor",
        classElement.getQualifiedName().toString());
    return false;
  }
}
```

Мы добавили метод `isValidClass()`, который проверяет соблюдение
наших правил:
* Класс должен быть общедоступным:
`classElement.getModifiers().Contains(Modifier.PUBLIC)`
*Класс не может быть абстрактным:
`classElement.getModifiers().Contains(Modifier.ABSTRACT)`
*Класс должен быть подклассом или реализовывать класс,
как указано в `@Factory.type()`. Сначала мы используем
`elementUtils.getTypeElement(item.getQualifiedFactoryGroupName ())`,
чтобы создать Элемент переданного класса
(`@Factory.type()`). Да, вы поняли, вы можете создать `TypeElement`
(с `TypeMirror`), просто зная квалифицированное имя класса.
Затем мы проверяем, интерфейс это или класс:
`superClassElement.getKind() == ElementKind.INTERFACE`.
Итак, у нас есть два случая: если это интерфейсы, то 
`classElement.getInterfaces().Contains(superClassElement.asType())`.
Если это класс, то мы должны сканировать иерархию наследования с
помощью вызова `currentClass.getSuperclass()`. Обратите внимание,
что эту проверку также можно выполнить с помощью
`typeUtils.isSubtype()`.
* У класса должен быть общедоступный пустой конструктор:
поэтому мы перебираем все вложенные элементы
`classElement.getEnclosedElements()` и проверяем
`ElementKind.CONSTRUCTOR`, `Modifier.PUBLIC` и 
`constructorElement.getParameters().Size() == 0`
* Если эти условия выполнены, `isValidClass()` возвращает `true`,
в противном случае выводит сообщение об ошибке и возвращает `false`.