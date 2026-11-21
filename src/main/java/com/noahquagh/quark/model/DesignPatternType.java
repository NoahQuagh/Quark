package com.noahquagh.quark.model;

import java.util.List;
import java.util.Optional;

import static com.noahquagh.quark.model.PatternCategory.*;
import static com.noahquagh.quark.model.PatternRole.*;

/**
 * Les 23 design patterns du GoF:
 * catégorie, description, et la liste des rôles (classes/interfaces) avec leurs
 * noms, relations (extends / implements / uses) et méthodes.
 *
 * @author noahq
 * @version 1.2
 */
public enum DesignPatternType {

    //CREATIONAL

    ABSTRACT_FACTORY(CREATIONAL,
            "Crée des familles d'objets liés sans spécifier leurs classes concrètes.",
            iface("ABSTRACT_FACTORY", "{Name}Factory", "Déclare les méthodes de création de chaque produit",
                    "{ABSTRACT_PRODUCT_A} createProductA()",
                    "{ABSTRACT_PRODUCT_B} createProductB()"),
            concreteClass("CONCRETE_FACTORY", "Concrete{Name}Factory{Index}", "Crée une famille de produits")
                    .extending("ABSTRACT_FACTORY").many(),
            iface("ABSTRACT_PRODUCT_A", "{Name}ProductA", "Interface du produit A", "void operation()"),
            iface("ABSTRACT_PRODUCT_B", "{Name}ProductB", "Interface du produit B", "void operation()"),
            concreteClass("CONCRETE_PRODUCT_A", "Concrete{Name}ProductA{Index}", "Variante concrète du produit A")
                    .extending("ABSTRACT_PRODUCT_A").many(),
            concreteClass("CONCRETE_PRODUCT_B", "Concrete{Name}ProductB{Index}", "Variante concrète du produit B")
                    .extending("ABSTRACT_PRODUCT_B").many(),
            concreteClass("CLIENT", "{Name}Client", "Utilise uniquement les interfaces abstraites")
                    .uses("ABSTRACT_FACTORY").optional()),

    BUILDER(CREATIONAL,
            "Sépare la construction d'un objet complexe de sa représentation.",
            iface("BUILDER", "{Name}Builder", "Définit les étapes de construction",
                    "void reset()", "void buildPartA()", "void buildPartB()", "{PRODUCT} getResult()"),
            concreteClass("CONCRETE_BUILDER", "Concrete{Name}Builder{Letter}", "Implémente les étapes de construction")
                    .extending("BUILDER").uses("PRODUCT").many(),
            concreteClass("PRODUCT", "{Name}", "Objet complexe à construire"),
            concreteClass("DIRECTOR", "{Name}Director", "Orchestre l'ordre des étapes",
                    "void construct()")
                    .uses("BUILDER").optional()),

    FACTORY_METHOD(CREATIONAL,
            "Délègue l'instanciation aux sous-classes via une méthode de fabrication.",
            iface("PRODUCT", "{Name}Product", "Interface des objets créés", "void operation()"),
            concreteClass("CONCRETE_PRODUCT", "Concrete{Name}Product{Letter}", "Produit concret")
                    .extending("PRODUCT").many(),
            abstractClass("CREATOR", "{Name}Creator", "Déclare la méthode de fabrication",
                    "abstract {PRODUCT} createProduct()", "void someOperation()")
                    .uses("PRODUCT"),
            concreteClass("CONCRETE_CREATOR", "Concrete{Name}Creator{Letter}", "Retourne un produit concret")
                    .extending("CREATOR").many()),

    PROTOTYPE(CREATIONAL,
            "Crée de nouveaux objets en clonant un exemplaire existant.",
            iface("PROTOTYPE", "{Name}Prototype", "Déclare l'opération de clonage",
                    "{PROTOTYPE} copy()"),
            concreteClass("CONCRETE_PROTOTYPE", "Concrete{Name}Prototype{Letter}", "Implémente le clonage")
                    .extending("PROTOTYPE").many(),
            concreteClass("REGISTRY", "{Name}PrototypeRegistry", "Stocke des prototypes prêts à cloner",
                    "void register(String key, {PROTOTYPE} prototype)",
                    "{PROTOTYPE} get(String key)")
                    .uses("PROTOTYPE").optional()),

    SINGLETON(CREATIONAL,
            "Garantit une instance unique avec un point d'accès global.",
            concreteClass("SINGLETON", "{Name}Singleton", "Classe à instance unique",
                    "static {SINGLETON} getInstance()")),

    //STRUCTURAL

    ADAPTER(STRUCTURAL,
            "Convertit l'interface d'une classe en une autre attendue par le client.",
            iface("TARGET", "{Name}Target", "Interface attendue par le client", "void request()"),
            concreteClass("ADAPTEE", "{Name}Adaptee", "Classe existante à adapter", "void specificRequest()"),
            concreteClass("ADAPTER", "{Name}Adapter", "Traduit Target vers Adaptee", "void request()")
                    .extending("TARGET").uses("ADAPTEE"),
            concreteClass("CLIENT", "{Name}Client", "Utilise l'interface Target").uses("TARGET").optional()),

    BRIDGE(STRUCTURAL,
            "Découple une abstraction de son implémentation pour les faire varier indépendamment.",
            abstractClass("ABSTRACTION", "{Name}Abstraction", "Abstraction de haut niveau",
                    "void operation()")
                    .uses("IMPLEMENTOR"),
            concreteClass("REFINED_ABSTRACTION", "Refined{Name}Abstraction{Letter}", "Variante de l'abstraction")
                    .extending("ABSTRACTION").many(),
            iface("IMPLEMENTOR", "{Name}Implementor", "Interface d'implémentation", "void operationImpl()"),
            concreteClass("CONCRETE_IMPLEMENTOR", "Concrete{Name}Implementor{Letter}", "Implémentation concrète")
                    .extending("IMPLEMENTOR").many()),

    COMPOSITE(STRUCTURAL,
            "Compose des objets en arbres et les traite de manière uniforme.",
            iface("COMPONENT", "{Name}Component", "Interface commune feuilles/composites", "void operation()"),
            concreteClass("LEAF", "{Name}Leaf", "Élément terminal", "void operation()")
                    .extending("COMPONENT"),
            concreteClass("COMPOSITE", "{Name}Composite", "Élément contenant des enfants",
                    "void operation()",
                    "void add({COMPONENT} component)",
                    "void remove({COMPONENT} component)",
                    "List<{COMPONENT}> getChildren()")
                    .extending("COMPONENT").uses("COMPONENT")),

    DECORATOR(STRUCTURAL,
            "Ajoute dynamiquement des responsabilités à un objet.",
            iface("COMPONENT", "{Name}Component", "Interface commune", "void operation()"),
            concreteClass("CONCRETE_COMPONENT", "Concrete{Name}Component", "Objet de base décoré",
                    "void operation()")
                    .extending("COMPONENT"),
            abstractClass("BASE_DECORATOR", "{Name}Decorator", "Délègue au composant enveloppé",
                    "void operation()")
                    .extending("COMPONENT").uses("COMPONENT"),
            concreteClass("CONCRETE_DECORATOR", "Concrete{Name}Decorator{Letter}", "Ajoute un comportement")
                    .extending("BASE_DECORATOR").many()),

    FACADE(STRUCTURAL,
            "Fournit une interface simplifiée à un sous-système complexe.",
            concreteClass("FACADE", "{Name}Facade", "Point d'entrée simplifié", "void operation()")
                    .uses("SUBSYSTEM_A", "SUBSYSTEM_B"),
            concreteClass("SUBSYSTEM_A", "{Name}SubsystemA", "Classe du sous-système", "void operationA()"),
            concreteClass("SUBSYSTEM_B", "{Name}SubsystemB", "Classe du sous-système", "void operationB()")),

    FLYWEIGHT(STRUCTURAL,
            "Partage des objets fins pour économiser la mémoire.",
            iface("FLYWEIGHT", "{Name}Flyweight", "Interface des objets partagés",
                    "void operation(String extrinsicState)"),
            concreteClass("CONCRETE_FLYWEIGHT", "Concrete{Name}Flyweight", "Stocke l'état intrinsèque",
                    "void operation(String extrinsicState)")
                    .extending("FLYWEIGHT"),
            concreteClass("FLYWEIGHT_FACTORY", "{Name}FlyweightFactory", "Crée et met en cache les flyweights",
                    "{FLYWEIGHT} getFlyweight(String key)")
                    .uses("FLYWEIGHT")),

    PROXY(STRUCTURAL,
            "Fournit un substitut contrôlant l'accès à un autre objet.",
            iface("SUBJECT", "{Name}Subject", "Interface commune", "void request()"),
            concreteClass("REAL_SUBJECT", "Real{Name}Subject", "Objet réel", "void request()")
                    .extending("SUBJECT"),
            concreteClass("PROXY", "{Name}Proxy", "Contrôle l'accès au sujet réel", "void request()")
                    .extending("SUBJECT").uses("REAL_SUBJECT")),

    //BEHAVIORAL

    CHAIN_OF_RESPONSIBILITY(BEHAVIORAL,
            "Passe une requête le long d'une chaîne de handlers.",
            abstractClass("HANDLER", "{Name}Handler", "Maillon de la chaîne",
                    "void setNext({HANDLER} next)",
                    "abstract void handle({REQUEST} request)")
                    .uses("HANDLER", "REQUEST"),
            concreteClass("CONCRETE_HANDLER", "Concrete{Name}Handler{Letter}", "Traite ou transmet la requête",
                    "void handle({REQUEST} request)")
                    .extending("HANDLER").many(),
            concreteClass("REQUEST", "{Name}Request", "Requête à traiter")),

    COMMAND(BEHAVIORAL,
            "Encapsule une requête dans un objet (file, annulation, journalisation).",
            iface("COMMAND", "{Name}Command", "Interface de commande", "void execute()"),
            concreteClass("CONCRETE_COMMAND", "Concrete{Name}Command{Letter}", "Lie un récepteur à une action",
                    "void execute()")
                    .extending("COMMAND").uses("RECEIVER").many(),
            concreteClass("RECEIVER", "{Name}Receiver", "Exécute le travail réel", "void action()"),
            concreteClass("INVOKER", "{Name}Invoker", "Déclenche les commandes",
                    "void setCommand({COMMAND} command)", "void invoke()")
                    .uses("COMMAND")),

    INTERPRETER(BEHAVIORAL,
            "Définit une grammaire et un interpréteur pour ses phrases.",
            iface("ABSTRACT_EXPRESSION", "{Name}Expression", "Expression de la grammaire",
                    "Object interpret({CONTEXT} context)"),
            concreteClass("TERMINAL_EXPRESSION", "Terminal{Name}Expression{Letter}", "Symbole terminal",
                    "Object interpret({CONTEXT} context)")
                    .extending("ABSTRACT_EXPRESSION").many(),
            concreteClass("NONTERMINAL_EXPRESSION", "NonTerminal{Name}Expression{Letter}", "Règle composée",
                    "Object interpret({CONTEXT} context)")
                    .extending("ABSTRACT_EXPRESSION").uses("ABSTRACT_EXPRESSION").many(),
            concreteClass("CONTEXT", "{Name}Context", "Informations globales d'interprétation")),

    ITERATOR(BEHAVIORAL,
            "Parcourt une collection sans exposer sa structure interne.",
            iface("ITERATOR", "{Name}Iterator", "Interface de parcours",
                    "boolean hasNext()", "Object next()"),
            concreteClass("CONCRETE_ITERATOR", "Concrete{Name}Iterator", "Parcours concret",
                    "boolean hasNext()", "Object next()")
                    .extending("ITERATOR").uses("CONCRETE_AGGREGATE"),
            iface("AGGREGATE", "{Name}Aggregate", "Collection itérable",
                    "{ITERATOR} createIterator()"),
            concreteClass("CONCRETE_AGGREGATE", "Concrete{Name}Aggregate", "Collection concrète",
                    "{ITERATOR} createIterator()")
                    .extending("AGGREGATE")),

    MEDIATOR(BEHAVIORAL,
            "Centralise les communications entre objets pour réduire leur couplage.",
            iface("MEDIATOR", "{Name}Mediator", "Interface du médiateur",
                    "void notify({COLLEAGUE} sender, String event)"),
            concreteClass("CONCRETE_MEDIATOR", "Concrete{Name}Mediator", "Coordonne les collègues",
                    "void notify({COLLEAGUE} sender, String event)")
                    .extending("MEDIATOR").uses("CONCRETE_COLLEAGUE"),
            abstractClass("COLLEAGUE", "{Name}Colleague", "Participant communiquant via le médiateur")
                    .uses("MEDIATOR"),
            concreteClass("CONCRETE_COLLEAGUE", "Concrete{Name}Colleague{Letter}", "Collègue concret")
                    .extending("COLLEAGUE").many()),

    MEMENTO(BEHAVIORAL,
            "Sauvegarde et restaure l'état d'un objet sans violer l'encapsulation.",
            concreteClass("ORIGINATOR", "{Name}Originator", "Objet dont l'état est sauvegardé",
                    "{MEMENTO} save()", "void restore({MEMENTO} memento)")
                    .uses("MEMENTO"),
            concreteClass("MEMENTO", "{Name}Memento", "Instantané immuable de l'état"),
            concreteClass("CARETAKER", "{Name}Caretaker", "Garde l'historique des mementos",
                    "void backup()", "void undo()")
                    .uses("ORIGINATOR", "MEMENTO")),

    OBSERVER(BEHAVIORAL,
            "Notifie automatiquement des observateurs d'un changement d'état.",
            iface("SUBJECT", "{Name}Subject", "Sujet observable",
                    "void attach({OBSERVER} observer)",
                    "void detach({OBSERVER} observer)",
                    "void notifyObservers()"),
            iface("OBSERVER", "{Name}Observer", "Interface des observateurs", "void update()"),
            concreteClass("CONCRETE_SUBJECT", "Concrete{Name}Subject", "Sujet concret avec état",
                    "void attach({OBSERVER} observer)",
                    "void detach({OBSERVER} observer)",
                    "void notifyObservers()")
                    .extending("SUBJECT").uses("OBSERVER"),
            concreteClass("CONCRETE_OBSERVER", "Concrete{Name}Observer{Letter}", "Réagit aux changements",
                    "void update()")
                    .extending("OBSERVER").uses("CONCRETE_SUBJECT").many()),

    STATE(BEHAVIORAL,
            "Change le comportement d'un objet selon son état interne.",
            concreteClass("CONTEXT", "{Name}Context", "Délègue au state courant",
                    "void setState({STATE} state)", "void request()")
                    .uses("STATE"),
            iface("STATE", "{Name}State", "Interface d'état", "void handle({CONTEXT} context)")
                    .uses("CONTEXT"),
            concreteClass("CONCRETE_STATE", "Concrete{Name}State{Letter}", "Comportement d'un état",
                    "void handle({CONTEXT} context)")
                    .extending("STATE").many()),

    STRATEGY(BEHAVIORAL,
            "Encapsule une famille d'algorithmes interchangeables.",
            iface("STRATEGY", "{Name}Strategy", "Interface commune des algorithmes", "void execute()"),
            concreteClass("CONCRETE_STRATEGY", "Concrete{Name}Strategy{Letter}", "Algorithme concret",
                    "void execute()")
                    .extending("STRATEGY").many(),
            concreteClass("CONTEXT", "{Name}Context", "Utilise une stratégie",
                    "void setStrategy({STRATEGY} strategy)", "void executeStrategy()")
                    .uses("STRATEGY")),

    TEMPLATE_METHOD(BEHAVIORAL,
            "Définit le squelette d'un algorithme, les sous-classes complètent les étapes.",
            abstractClass("ABSTRACT_CLASS", "{Name}Template", "Contient l'algorithme et les étapes",
                    "final void templateMethod()",
                    "abstract void step1()",
                    "abstract void step2()",
                    "void hook()"),
            concreteClass("CONCRETE_CLASS", "Concrete{Name}Template{Letter}", "Implémente les étapes",
                    "void step1()", "void step2()")
                    .extending("ABSTRACT_CLASS").many()),

    VISITOR(BEHAVIORAL,
            "Ajoute des opérations à une hiérarchie d'objets sans la modifier.",
            iface("VISITOR", "{Name}Visitor", "Une méthode visit par élément concret",
                    "void visit({CONCRETE_ELEMENT} element)"),
            concreteClass("CONCRETE_VISITOR", "Concrete{Name}Visitor{Letter}", "Opération concrète",
                    "void visit({CONCRETE_ELEMENT} element)")
                    .extending("VISITOR").many(),
            iface("ELEMENT", "{Name}Element", "Élément acceptant un visiteur",
                    "void accept({VISITOR} visitor)"),
            concreteClass("CONCRETE_ELEMENT", "Concrete{Name}Element{Letter}", "Élément concret",
                    "void accept({VISITOR} visitor)")
                    .extending("ELEMENT").many());

    private final PatternCategory category;
    private final String description;
    private final List<PatternRole> roles;

    /**
     * Initialise un patron de conception du GoF avec sa catégorie, sa description et ses participants.
     *
     * @param category    catégorie GoF du patron ({@code CREATIONAL}, {@code STRUCTURAL}, {@code BEHAVIORAL})
     * @param description résumé pédagogique du rôle du patron
     * @param roles       liste ordonnée des rôles participants
     */
    DesignPatternType(PatternCategory category, String description, PatternRole... roles) {
        this.category = category;
        this.description = description;
        this.roles = List.of(roles);
    }

    /**
     * Retourne la catégorie GoF du patron de conception.
     *
     * @return catégorie {@link PatternCategory} associée
     */
    public PatternCategory getCategory() {
        return category;
    }

    /**
     * Retourne la description pédagogique de l'intention du patron.
     *
     * @return description textuelle
     */
    public String getDescription() {
        return description;
    }

    /**
     * Retourne tous les rôles (classes/interfaces) du pattern, dans l'ordre de génération conseillé.
     *
     * @return liste immuable des {@link PatternRole}
     */
    public List<PatternRole> getRoles() {
        return roles;
    }

    /**
     * Recherche un rôle participant précis par son identifiant interne.
     *
     * @param roleId identifiant du rôle recherché (ex : {@code "STRATEGY"})
     * @return un {@link Optional} contenant le rôle s'il existe
     */
    public Optional<PatternRole> getRole(String roleId) {
        return roles.stream().filter(r -> r.id().equals(roleId)).findFirst();
    }

    /**
     * Retourne un nom de sous-package normalisé en minuscules pour isoler les fichiers générés
     * (ex : {@code CHAIN_OF_RESPONSIBILITY} &rarr; {@code "chainofresponsibility"}).
     *
     * @return nom de sous-package valide en Java
     */
    public String getSubPackage() {
        return name().toLowerCase().replace("_", "");
    }

    /**
     * Formate le nom de la constante en titre lisible (ex : {@code CHAIN_OF_RESPONSIBILITY} &rarr; {@code "Chain Of Responsibility"}).
     * Utilise {@code name()} et non {@code toString()} pour éviter une récursion infinie.
     *
     * @return nom formaté pour l'affichage utilisateur
     */
    public String displayName() {
        String[] words = name().toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(' ');
            }
        }
        return result.toString().trim();
    }

    /**
     * Retourne le nom lisible du pattern pour l'affichage direct dans les composants Swing ({@link javax.swing.JComboBox}).
     *
     * @return nom formaté du patron
     */
    @Override
    public String toString() {
        return displayName();
    }
}