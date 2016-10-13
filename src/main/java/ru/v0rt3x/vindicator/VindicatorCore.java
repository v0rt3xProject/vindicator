package ru.v0rt3x.vindicator;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.v0rt3x.vindicator.common.*;
import ru.v0rt3x.vindicator.component.ComponentManager;
import ru.v0rt3x.vindicator.service.ServiceManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VindicatorCore {

    private final ArgParser.Args args;
    private final ConfigFile config;

    private final Reflections discoveryHelper;
    private final DataBase db;
    
    private final Map<Class<? extends GenericManager<?>>, GenericManager<?>> managers = new ConcurrentHashMap<>();
    private final ExecutorService threadPool;
    private final Map<String, Queue<?>> queueMap = new HashMap<>();

    private boolean isRunning = true;
    private boolean isReadyForShutDown = false;

    private static final Logger logger = LoggerFactory.getLogger(VindicatorCore.class);
    private static VindicatorCore instance;

    public VindicatorCore(ArgParser.Args commandArgs) throws IOException {
        args = commandArgs;

        config = new ConfigFile(
            new File(args.kwargs("config", "vindicator.ini"))
        );

        discoveryHelper = new Reflections(
            ClasspathHelper.forPackage("ru.v0rt3x"),
            new SubTypesScanner()
        );

        db = new DataBase(
            config.getString("vindicator.db.uri", "mongodb://127.0.0.1:27017/"),
            config.getString("vindicator.db.name", "vindicator")
        );

        threadPool = Executors.newWorkStealingPool(
            Runtime.getRuntime().availableProcessors() * config.getInt("vindicator.threads_per_cpu", 4)
        );

        registerShutdownHook();
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook()));
    }

    public void executeThread(Runnable runnable) {
        threadPool.submit(runnable);
    }

    public <T> Set<Class<? extends T>> discover(Class<T> parentClass) {
        return discoveryHelper.getSubTypesOf(parentClass);
    }

    public <T extends GenericManager<?>> void registerManager(Class<T> manager) {
        try {
            GenericManager<?> managerInstance = manager.newInstance();
            managers.put(manager, managerInstance);
            managerInstance.setUp();

            managerInstance.discover();
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Unable to instantiate manager", e);
        }
    }

    public <T extends GenericManager<?>> void shutdownManager(Class<T> manager) {
        if (managers.containsKey(manager)) {
            managers.get(manager).onShutDown();
            managers.remove(manager);
        }
    }

    public <T extends GenericManager<?>> T getManager(Class<T> manager) {
        if (managers.containsKey(manager)) {
            return manager.cast(managers.get(manager));
        }

        return null;
    }
    
    public static VindicatorCore getInstance() {
        return instance;
    }

    private void mainLoop() throws InterruptedException {
        logger.info("Vindicator started");

        executeThread(new ConfigWatch());

        registerManager(ComponentManager.class);
        registerManager(ServiceManager.class);

        while (isRunning) {
            Thread.sleep(1000);
        }

        logger.info("Vindicator is going to shutdown. Waiting for remaining operations...");

        shutdownManager(ServiceManager.class);
        shutdownManager(ComponentManager.class);

        isReadyForShutDown = true;

        logger.info("Vindicator is down...");
        Runtime.getRuntime().exit(0);
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void shutdown() {
        isRunning = false;
    }

    public boolean isReadyForShutDown() {
        return isReadyForShutDown;
    }

    public <T> boolean createQueue(Class<T> queueType, String queueName) {
        if (!queueMap.containsKey(queueName)) {
            logger.info("Creating queue: [{}] {}", queueType.getSimpleName(), queueName);
            queueMap.put(queueName, new Queue<T>());
            return true;
        }
        return false;
    }

    public Queue<?> getQueue(String queueName) {
        return queueMap.getOrDefault(queueName, null);
    }

    public DataBase getDataBase() {
        return db;
    }

    public static void main(String[] cmdline) throws InterruptedException {
        ArgParser.Args args = ArgParser.parse(cmdline);

        try {
            VindicatorCore.instance = new VindicatorCore(args);
        } catch (IOException e) {
            logger.error("Unable to open config file: {}", e.getMessage());
            System.exit(1);
        }

        VindicatorCore.instance.mainLoop();
    }

    public ConfigFile config() {
        return config;
    }
}
