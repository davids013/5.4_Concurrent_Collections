package task2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Результаты замеров скорости работы ConcurrentHashMap и многопоточной обёртки synchronizedMap():
 * 1. В однопоточном режиме ConcurrentHashMap большого размера проигрывает в скорости synchronizedMap()
 * 2. В многопоточном режиме чтение и перезапись ConcurrentHashMap эффективнее
 * 2.1. Преимущество в скорости ConcurrentHashMap растёт с увеличением потоков и увеличением размера массива.
 * 3. В многопоточном режиме добавление новых элементов ConcurrentHashMap эффективнее
 * 4. ConcurrentHashMap при вызове replaceAll() работает на порядок медленнее циклического прохода,
 *    в то время, как synchronizedMap() - эта функция эффективнее.
 */

public class Main_task2 {
    private static final int ARRAY_SIZE = 2_000_000;
    private static final int THREADS = Runtime.getRuntime().availableProcessors();

    public static void main(String[] args) {
        System.out.println("\n\tЗадача 2. Разница в производительности synchronizedMap() и ConcurrentHashMap\n");
        final String[] concurrentNames = {"synchronizedMap", "ConcurrentHashMap"};
        final Map<Integer, Integer> monoMap = Collections.synchronizedMap(new HashMap<>());
        final ConcurrentMap<Integer, Integer> multiMap = new ConcurrentHashMap<>();
        float overall = 0;
        long    monoTime,
                multiTime;

        monoTime = expansionTest(monoMap, 1);
        multiTime = expansionTest(multiMap, 1);
        printResults("расширения", concurrentNames, monoTime, multiTime, false);
        overall = (overall + (float) monoTime / multiTime) / 2;

        monoMap.clear();
        multiMap.clear();
        monoTime = expansionTest(monoMap, THREADS);
        multiTime = expansionTest(multiMap, THREADS);
        printResults("расширения", concurrentNames, monoTime, multiTime, true);
        overall = (overall + (float) monoTime / multiTime) / 2;

        monoTime = readTest(monoMap, THREADS);
        multiTime = readTest(multiMap, THREADS);
        printResults("чтения", concurrentNames, monoTime, multiTime, true);
        overall = (overall + (float) monoTime / multiTime) / 2;

        monoTime = writeTest(monoMap, THREADS);
        multiTime = writeTest(multiMap, THREADS);
        printResults("перезаписи", concurrentNames, monoTime, multiTime, true);
        overall = (overall + (float) monoTime / multiTime) / 2;

        System.out.printf("В проведенных тестах %s быстрее %s в %.1f раза\n",
                concurrentNames[1], concurrentNames[0], overall);
    }

    private static int[] getRandomIntArray() {
        int[] array = new int[ARRAY_SIZE];
        Random rnd = new Random();
        for (int i = 0; i < ARRAY_SIZE; i++)
            array[i] = rnd.nextInt();
        return array;
    }

    private static void readMap(Map<Integer, Integer> map) {
        for (Integer i : map.keySet()) map.get(i);
    }

    private static void writeMap(Map<Integer, Integer> map) {
//        map.replaceAll((i, v) -> i);
        for (Integer i : map.keySet()) map.put(i, i);
    }

    private static long expansionTest(Map<Integer, Integer> map, int threads) {
        System.out.printf("\tИдёт тест %sпоточного расширения...\n", (threads > 1) ? "много" : "одно");
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        final AtomicInteger counter = new AtomicInteger();
        int[] array = getRandomIntArray();
        long time = System.currentTimeMillis();
        if (threads > 0) {
            for (int i = 0; i < ARRAY_SIZE; i++) {
                final int temp = i;
                pool.submit(() -> {
                    map.put(temp, array[temp]);
                });
                counter.incrementAndGet();
            }
            while(counter.get() < ARRAY_SIZE);
            pool.shutdown();
        }
        return System.currentTimeMillis() - time;
    }

    private static long readTest(Map<Integer, Integer> map, int threads) {
        System.out.printf("\tИдёт тест %sпоточного чтения...\n", (threads > 1) ? "много" : "одно");
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        final AtomicInteger counter = new AtomicInteger();
        long time = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                readMap(map);
                counter.incrementAndGet();
            });
        }
        while(counter.get() < threads);
        pool.shutdown();
        return System.currentTimeMillis() - time;
    }

    private static long writeTest(Map<Integer, Integer> map, int threads) {
        System.out.printf("\tИдёт тест %sпоточной перезаписи...\n", (threads > 1) ? "много" : "одно");
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        final AtomicInteger counter = new AtomicInteger();
        long time = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                writeMap(map);
                counter.incrementAndGet();
            });
        }
        while(counter.get() < threads);
        pool.shutdown();
        return System.currentTimeMillis() - time;
    }

    public static void printResults(
            String op, String[] mapNames, long time1, long time2, boolean isMulti) {
        final String suffix = isMulti ? "много" : "одно";
        System.out.printf("Время %sпоточного(-ой) %s %s, мс:\t%d\n", suffix, op, mapNames[0], time1);
        System.out.printf("Время %sпоточного(-ой) %s %s, мс:\t%d\n", suffix, op, mapNames[1], time2);
        System.out.printf("%sTime / %sTime = %.1f\n", mapNames[0], mapNames[1], (float) time1 / time2);
    }
}
