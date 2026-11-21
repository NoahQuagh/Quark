# Quark — Générateur de Design Patterns pour IntelliJ IDEA

**Quark** est un plugin pour **IntelliJ IDEA** permettant de générer instantanément la structure complète des **23 patrons de conception du GoF** (*Gang of Four*) directement depuis le menu contextuel (`Alt + Insert`).

Conçu à la fois comme un outil de productivité pour les développeurs Java, **Quark** assemble automatiquement les classes, interfaces, relations d'héritage et signatures de méthodes dans le package actif.

---

## Fonctionnalités principales

* **Intégration native à l'IDE :** Accessible directement via `Alt + Insert` (ou clic droit $\rightarrow$ *New* $\rightarrow$ *Design Pattern*) sur n'importe quel package Java ou à l'intérieur d'un fichier existant.
* **Catalogue complet des 23 patterns GoF :**
    * **Création (Creational) :** *Abstract Factory, Builder, Factory Method, Prototype, Singleton*.
    * **Structure (Structural) :** *Adapter, Bridge, Composite, Decorator, Facade, Flyweight, Proxy*.
    * **Comportement (Behavioral) :** *Chain of Responsibility, Command, Interpreter, Iterator, Mediator, Memento, Observer, State, Strategy, Template Method, Visitor*.
* **Validation en temps réel :** Vérification instantanée de la validité de l'identifiant Java et de la convention *PascalCase* via l'API `PsiNameHelper` d'IntelliJ.
* **Mode Pédagogique :** Option activable ajoutant des commentaires Javadoc détaillés sur le rôle de chaque participant et l'intention globale du patron.
* **Code prêt à compiler :** Génération des imports requis, héritage automatique des méthodes d'interfaces et abstraites (`@Override`), valeurs de retour par défaut valides, et formatage automatique selon les règles du projet.

---

## Architecture & Conception

Le moteur de génération de **Quark** applique lui-même plusieurs patrons de conception afin de garantir un code modulaire, extensible et testable indépendamment de l'IDE :

* **Méta-modélisation déclarative (`model`) :** Chaque patron est défini de manière déclarative (`DesignPatternType`, `PatternRole`, `ClassKind`) avec ses participants, ses relations (`extends`, `implements`, `uses`) et ses placeholders dynamiques (`{Name}`, `{Letter}`, `{Index}`).
* **Pattern Builder (`JavaClassBuilder`) :** Encapsule la construction syntaxique d'un fichier source Java (package, imports, Javadoc, déclaration, champs, constructeurs, méthodes) via une API fluide.
* **Pattern Strategy (`PatternStrategy`) :** Permet de déléguer la logique de génération à une stratégie générique (`DefaultPatternStrategy`) ou à des stratégies spécialisées (`SingletonStrategy`, etc.).
* **Intégration PSI (`PsiFileWriter`) :** Isole toutes les opérations d'écriture dans le *Program Structure Interface* d'IntelliJ via `WriteCommandAction` et `CodeStyleManager` (support complet de l'annulation `Ctrl + Z`).

```text
src/main/java/com/noahquagh/quark/
├── actions/
│   └── CreateDesignPatternAction.java   # Point d'entrée (Alt + Insert)
├── model/
│   ├── ClassKind.java                   # Enum : INTERFACE, ABSTRACT_CLASS, CLASS
│   ├── DesignPatternType.java           # Définition des 23 patterns du GoF
│   ├── PatternCategory.java             # Enum : CREATIONAL, STRUCTURAL, BEHAVIORAL
│   └── PatternRole.java                 # Record modélisant un participant
├── ui/
│   └── DesignPatternDialog.java         # Boîte de dialogue modale (DialogWrapper)
└── generator/
    ├── GenerationContext.java           # Contexte immuable de génération
    ├── JavaClassBuilder.java            # Pattern Builder pour l'assemblage Java
    ├── PatternGenerator.java            # Orchestrateur principal
    ├── PsiFileWriter.java               # Écriture sécurisée dans le PSI IntelliJ
    └── strategy/
        ├── PatternStrategy.java         # Interface Strategy
        ├── DefaultPatternStrategy.java  # Génération générique basée sur les rôles
        └── SingletonStrategy.java       # Stratégie dédiée au Singleton
```

---

## Installation & Développement

### Prérequis
* **JDK 17** ou **JDK 21**
* **IntelliJ IDEA** (Community ou Ultimate, version 2024.2+)

### Lancer le plugin en mode développement (Bac à sable)
1. Cloner le dépôt :
   ```bash
   git clone https://github.com/NoahQuagh/Quark.git
   cd Quark
   ```
2. Lancer une instance d'IntelliJ IDEA préchargée avec le plugin via Gradle :
   ```bash
   ./gradlew runIde
   ```
3. Dans la fenêtre IntelliJ bac à sable qui s'ouvre, créer ou ouvrir un projet Java, sélectionner un package dans `src`, puis appuyer sur **`Alt + Insert`** $\rightarrow$ **`Design Pattern`**.

### Compiler le package d'installation (`.zip`)
Pour générer l'archive installable manuellement dans n'importe quel IntelliJ IDEA (*Settings $\rightarrow$ Plugins $\rightarrow$ ⚙️ $\rightarrow$ Install Plugin from Disk...*) :
```bash
./gradlew buildPlugin
```
L'archive est générée dans `build/distributions/`.

---

## Technologies utilisées

* **Langage :** Java 17+
* **SDK :** IntelliJ Platform SDK (PSI, `DialogWrapper`, `AnAction`, `WriteCommandAction`)
* **Build :** Gradle avec le plugin `org.jetbrains.intellij.platform`

---

## Licence

Projet développé par **Noah Quaghebeur**.