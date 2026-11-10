package java.lang.management;

// stub para evitar NoClassDefFoundError en android art al usar driver jdbc de postgresql
public class ManagementFactory
{
    private static final MemoryMXBean memoryMXBean = new MemoryMXBean()
    {
        @Override
        public MemoryUsage getHeapMemoryUsage()
        {
            long max = Runtime.getRuntime().maxMemory();
            long total = Runtime.getRuntime().totalMemory();
            long used = total - Runtime.getRuntime().freeMemory();
            return new MemoryUsage(0, used, total, max);
        }

        @Override
        public MemoryUsage getNonHeapMemoryUsage()
        {
            return new MemoryUsage(0, 0, 0, 0);
        }
    };

    public static MemoryMXBean getMemoryMXBean()
    {
        return memoryMXBean;
    }
}
