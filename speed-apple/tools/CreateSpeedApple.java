import net.mcreator.Launcher;
import net.mcreator.blockly.data.BlocklyLoader;
import net.mcreator.blockly.data.Dependency;
import net.mcreator.element.GeneratableElement;
import net.mcreator.element.ModElementType;
import net.mcreator.element.ModElementTypeLoader;
import net.mcreator.element.parts.*;
import net.mcreator.element.parts.procedure.LogicProcedure;
import net.mcreator.element.parts.procedure.StringListProcedure;
import net.mcreator.element.types.Item;
import net.mcreator.element.types.Recipe;
import net.mcreator.generator.Generator;
import net.mcreator.generator.GeneratorConfiguration;
import net.mcreator.generator.setup.WorkspaceGeneratorSetup;
import net.mcreator.gradle.GradleUtils;
import net.mcreator.io.net.analytics.GoogleAnalytics;
import net.mcreator.minecraft.DataListLoader;
import net.mcreator.plugin.PluginLoader;
import net.mcreator.plugin.modapis.ModAPIManager;
import net.mcreator.preferences.PreferencesManager;
import net.mcreator.ui.init.*;
import net.mcreator.ui.laf.themes.ThemeManager;
import net.mcreator.util.MCreatorVersionNumber;
import net.mcreator.util.TerribleModuleHacks;
import net.mcreator.workspace.Workspace;
import net.mcreator.workspace.elements.ModElement;
import net.mcreator.workspace.elements.VariableTypeLoader;
import net.mcreator.workspace.settings.WorkspaceSettings;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/** Creates this editable project through MCreator 2026.2's own data models and generator. */
public class CreateSpeedApple {
    public static void main(String[] args) {
        try {
            create(Path.of(args[0]));
            System.out.println("SPEED_APPLE_WORKSPACE_CREATED");
            System.exit(0);
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }

    private static void create(Path target) throws Exception {
        GoogleAnalytics.ANALYTICS_ENABLED = false;
        TerribleModuleHacks.openAllFor(ClassLoader.getSystemClassLoader().getUnnamedModule());
        TerribleModuleHacks.openMCreatorRequirements();
        System.setProperty("jdk.xml.maxElementDepth", "0");
        Properties version = new Properties();
        version.load(Launcher.class.getResourceAsStream("/mcreator.conf"));
        Launcher.version = new MCreatorVersionNumber(version);
        PreferencesManager.init();
        PluginLoader.initInstance();
        ThemeManager.loadThemes();
        UIRES.preloadImages();
        ThemeManager.applySelectedTheme();
        DataListLoader.preloadCache();
        L10N.initTranslations();
        ModAPIManager.initAPIs();
        VariableTypeLoader.loadVariableTypes();
        BlocklyJavaScriptsLoader.init();
        BlocklyToolboxesLoader.init();
        BlocklyLoader.init();
        ModElementTypeLoader.loadModElements();
        for (String name : PluginLoader.INSTANCE.getResources(Pattern.compile("generator\\.yaml"))) {
            String generator = name.replace("/generator.yaml", "");
            Generator.GENERATOR_CACHE.put(generator, new GeneratorConfiguration(generator));
        }
        if (!Generator.GENERATOR_CACHE.containsKey("fabric-26.2"))
            throw new IllegalStateException("The Fabric 26.2 generator is not installed.");
        if (Files.exists(target.resolve("speedapple.mcreator")))
            throw new IllegalStateException("A workspace already exists at the target. Refusing to replace it.");

        WorkspaceSettings settings = new WorkspaceSettings("speedapple");
        settings.setModName("Speed Apple");
        settings.setVersion("1.0.0");
        settings.setDescription("A craftable apple that grants Speed I for 10 seconds after eating.");
        settings.setAuthor("Player");
        settings.setLicense("All Rights Reserved");
        settings.setWebsiteURL("");
        settings.setUpdateURL("");
        settings.setCurrentGenerator("fabric-26.2");
        settings.setModElementsPackage("net.mcreator.speedapple");
        try (Workspace workspace = Workspace.createWorkspace(target.resolve("speedapple.mcreator").toFile(), settings)) {
            WorkspaceGeneratorSetup.setupWorkspaceBase(workspace);

            String speed = DataListLoader.loadDataList("effects").stream()
                .map(entry -> entry.getName())
                .filter(name -> name.equalsIgnoreCase("speed") || name.toLowerCase(Locale.ROOT).contains("movement_speed"))
                .findFirst().orElseThrow(() -> new IllegalStateException("Speed effect mapping is missing."));

            ModElement eatenElement = new ModElement(workspace, "SpeedAppleEaten", ModElementType.PROCEDURE);
            eatenElement.putMetadata("dependencies", List.of(new Dependency("entity", "entity")));
            var eaten = new net.mcreator.element.types.Procedure(eatenElement);
            eaten.procedurexml = """
                <xml xmlns="https://developers.google.com/blockly/xml">
                  <block type="event_trigger" deletable="false" x="40" y="40">
                    <field name="trigger">no_ext_trigger</field>
                    <next><block type="entity_add_potion">
                      <field name="potion">%s</field>
                      <value name="entity"><block type="entity_from_deps"/></value>
                      <value name="duration"><block type="math_number"><field name="NUM">200</field></block></value>
                      <value name="level"><block type="math_number"><field name="NUM">0</field></block></value>
                    </block></next>
                  </block>
                </xml>
                """.formatted(speed);
            store(workspace, eaten);

            ModElement appleElement = new ModElement(workspace, "SpeedApple", ModElementType.ITEM);
            Item apple = new Item(appleElement);
            apple.name = "加速苹果";
            apple.texture = new TextureHolder(workspace, "minecraft:apple");
            apple.guiTexture = new TextureHolder(workspace, "");
            apple.renderType = 0;
            apple.customModelName = "Normal";
            apple.creativeTabs = List.of(new TabEntry(workspace, "FOOD"));
            apple.stackSize = 64;
            apple.useDuration = 32;
            apple.toolType = 1;
            apple.inventorySize = 0;
            apple.recipeRemainder = new MItemBlock(workspace, "");
            apple.eatResultItem = new MItemBlock(workspace, "");
            apple.isFood = true;
            apple.nutritionalValue = 4;
            apple.saturation = 0.3;
            apple.isAlwaysEdible = true;
            apple.animation = new ItemUseAnimation(workspace, "eat");
            apple.glowCondition = new LogicProcedure(null, false);
            apple.openGUIOnRightClick = new LogicProcedure(null, false);
            apple.specialInformation = new StringListProcedure(null, List.of("吃下后获得速度Ⅰ，持续10秒"));
            apple.onFinishUsingItem = new net.mcreator.element.parts.procedure.Procedure("SpeedAppleEaten");
            store(workspace, apple);

            ModElement recipeElement = new ModElement(workspace, "SpeedAppleRecipe", ModElementType.RECIPE);
            Recipe recipe = new Recipe(recipeElement);
            recipe.recipeType = "Crafting";
            recipe.recipeShapeless = true;
            recipe.group = "speedapple";
            recipe.recipeSlots = new MItemBlock[2];
            recipe.recipeSlots[0] = new MItemBlock(workspace, "Items.APPLE");
            recipe.recipeSlots[1] = new MItemBlock(workspace, "Items.SUGAR");
            recipe.recipeReturnStack = new MItemBlock(workspace, "CUSTOM:SpeedApple");
            recipe.recipeRetstackSize = 1;
            recipe.unlockingItems = List.of(new MItemBlock(workspace, "Items.APPLE"));
            store(workspace, recipe);

            GradleUtils.updateMCreatorBuildFile(workspace);
            System.out.println("Preparing the Minecraft/Fabric development dependencies...");
            workspace.getGenerator().reloadGradleCaches();

            for (GeneratableElement element : List.of(eaten, apple, recipe)) {
                if (!workspace.getGenerator().generateElement(element))
                    throw new IllegalStateException("Generation failed: " + element.getModElement().getName());
            }
            if (!workspace.getGenerator().generateBase())
                throw new IllegalStateException("Workspace base generation failed.");
            workspace.getFileManager().saveWorkspaceDirectlyAndWait();
        }
    }

    private static void store(Workspace workspace, GeneratableElement element) {
        workspace.addModElement(element.getModElement());
        workspace.getModElementManager().storeModElement(element);
    }
}
